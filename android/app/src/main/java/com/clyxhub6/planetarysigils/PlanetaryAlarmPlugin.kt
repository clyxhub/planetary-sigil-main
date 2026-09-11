package com.clyxhub6.planetarysigils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import java.io.File
import java.io.FileOutputStream

/**
 * Native planetary-hour alarm, reminder and sigil-save bridge.
 *
 * Reliability handled here so the three features survive backgrounding, Doze,
 * app close and reboot:
 *
 *  1. Exact alarms use setExactAndAllowWhileIdle so they fire at the configured
 *     planetary hour even in Doze. Exact-alarm permission (API 31+) is checked
 *     up-front via SCHEDULE_EXACT_ALARM / USE_EXACT_ALARM.
 *  2. Every scheduled alarm/reminder is persisted in AlarmStore and re-registered
 *     by BootReceiver after boot / timezone / app-replaced events (idempotent,
 *     no duplicates).
 *  3. Notification permission (API 33+) is requested before resolving the call and
 *     the real post-dialog result is returned via the Capacitor permission callback.
 *  4. Sigil files are written to user-visible storage: MediaStore (Pictures) on
 *     API 29+; legacy public Downloads directory on API 22-28.
 */
@CapacitorPlugin(
    name = "PlanetaryAlarm",
    permissions = [
        Permission(alias = "notifications", strings = [Manifest.permission.POST_NOTIFICATIONS]),
        Permission(alias = "storage", strings = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    ]
)
class PlanetaryAlarmPlugin : Plugin() {

    override fun load() {
        super.load()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(ALARM_CHANNEL_ID, "Planetary Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Full-screen alarms for planetary hours"
                    enableVibration(true)
                    setBypassDnd(true)
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(REMINDER_CHANNEL_ID, "Planetary Hour Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Heads-up notification before a planetary hour"
                    enableVibration(true)
                }
            )
        }
    }

    // ── Notification Permission (API 33+) ──────────────────────────────────

    @PluginMethod
    fun hasNotificationPermission(call: PluginCall) {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        val ret = JSObject(); ret.put("value", granted); call.resolve(ret)
    }

    @PluginMethod
    fun requestNotificationPermission(call: PluginCall) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                val ret = JSObject(); ret.put("value", true); call.resolve(ret)
            } else {
                requestPermissionForAlias("notifications", call, NOTIFICATION_CALLBACK)
            }
        } else {
            val ret = JSObject(); ret.put("value", true); call.resolve(ret)
        }
    }

    // Capacitor always invokes permission callbacks with the saved PluginCall.
    @PermissionCallback
    private fun notificationsGranted(call: PluginCall) {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "POST_NOTIFICATIONS result: $granted")
        val ret = JSObject(); ret.put("value", granted); call.resolve(ret)
    }

    // ── Exact Alarm Permission ─────────────────────────────────────────────

    @PluginMethod
    fun hasExactAlarmPermission(call: PluginCall) {
        val ret = JSObject()
        ret.put("value", Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms())
        call.resolve(ret)
    }

    @PluginMethod
    fun requestExactAlarmPermission(call: PluginCall) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
        val ret = JSObject(); ret.put("value", true); call.resolve(ret)
    }

    // ── Schedule / Cancel ──────────────────────────────────────────────────

    @PluginMethod
    fun schedule(call: PluginCall) {
        val timestamp = call.getLong("timestamp")?.toLong() ?: call.getString("timestamp")?.toLongOrNull()
        val planetName = call.getString("planetName")
        val leadMinutes = call.getInt("leadMinutes") ?: 0
        if (timestamp == null || planetName == null) { call.reject("Must provide timestamp and planetName"); return }

        // Persist first (single source of truth). save() upserts, so pressing a
        // bell twice never creates a duplicate OS alarm.
        AlarmStore.save(context, AlarmStore.Alarm(timestamp, planetName, leadMinutes))

        scheduleAlarm(context, timestamp, planetName)
        if (leadMinutes > 0) scheduleReminder(context, timestamp, planetName, leadMinutes.toLong())

        val ret = JSObject(); ret.put("success", true); call.resolve(ret)
        Log.d(TAG, "Scheduled $planetName @ $timestamp lead=$leadMinutes (schedule)")
    }

    @PluginMethod
    fun cancel(call: PluginCall) {
        val timestamp = call.getLong("timestamp")?.toLong() ?: call.getString("timestamp")?.toLongOrNull() ?: return
        cancelAlarm(context, timestamp)
        AlarmStore.remove(context, timestamp)
        val ret = JSObject(); ret.put("success", true); call.resolve(ret)
        Log.d(TAG, "Cancelled @ $timestamp")
    }

    // ── Sigil save: user-visible storage ───────────────────────────────────

    @PluginMethod
    fun saveFile(call: PluginCall) {
        val dataUrl = call.getString("dataUrl")
        val svg = call.getString("svg")
        val fileName = call.getString("fileName") ?: "sigil.png"
        if (dataUrl == null && svg == null) { call.reject("dataUrl or svg required"); return }

        val isSvg = fileName.endsWith("svg", true)
        val mime = when {
            isSvg -> "image/svg+xml"
            fileName.endsWith("jpg", true) || fileName.endsWith("jpeg", true) -> "image/jpeg"
            else -> "image/png"
        }
        val bytes = if (svg != null) svg.toByteArray(Charsets.UTF_8) else {
            try { Base64.decode(dataUrl!!.substringAfter(","), Base64.DEFAULT) } catch (e: Exception) { ByteArray(0) }
        }
        if (bytes.isEmpty()) { call.reject("Could not decode image data"); return }

        // API 29+ writes through MediaStore with no permission. On API 22-28 the
        // public Pictures/Downloads write needs WRITE_EXTERNAL_STORAGE granted at
        // runtime (declared with maxSdkVersion=28), so request it when needed and
        // finish the save from the permission callback.
        val needLegacyPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needLegacyPermission) {
            pendingSaveBytes = bytes
            pendingSaveMime = mime
            pendingSaveName = fileName
            pendingSaveIsSvg = isSvg
            requestPermissionForAlias("storage", call, STORAGE_CALLBACK)
            return
        }

        performSave(call, bytes, mime, fileName, isSvg)
    }

    private fun performSave(call: PluginCall, bytes: ByteArray, mime: String, fileName: String, isSvg: Boolean) {
        try {
            val savedFile = writeToUserStorage(context, bytes, mime, fileName, isSvg)
            val ret = JSObject()
            ret.put("success", true)
            ret.put("path", savedFile)
            ret.put("displayName", fileName)
            call.resolve(ret)
            Log.d(TAG, "Saved sigil -> $savedFile")
        } catch (e: Exception) {
            Log.e(TAG, "saveFile failed", e)
            call.reject(e.message ?: "Could not save file")
        }
    }

    @PermissionCallback
    private fun storageGranted(call: PluginCall) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (granted && pendingSaveBytes != null && pendingSaveMime != null && pendingSaveName != null) {
            performSave(call, pendingSaveBytes!!, pendingSaveMime!!, pendingSaveName!!, pendingSaveIsSvg)
        } else {
            call.reject("Storage permission denied — cannot save the sigil")
        }
        pendingSaveBytes = null; pendingSaveMime = null; pendingSaveName = null
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

    companion object {
        private const val TAG = "PlanetaryAlarm"
        const val ALARM_CHANNEL_ID = "planetary_alarm_channel"
        const val REMINDER_CHANNEL_ID = "planetary_reminder_channel"
        const val REMINDER_ACTION = "com.clyxhub6.planetarysigils.REMINDER"
        const val NOTIFICATION_CALLBACK = "notificationsGranted"
        const val STORAGE_CALLBACK = "storageGranted"
        private const val ALARM_SEED = 700 // distinguishes alarm vs reminder PendingIntents

        // Deferred legacy (API 22-28) save payload, held while the
        // WRITE_EXTERNAL_STORAGE prompt is shown; the call itself is passed back
        // to storageGranted() by Capacitor.
        private var pendingSaveBytes: ByteArray? = null
        private var pendingSaveMime: String? = null
        private var pendingSaveName: String? = null
        private var pendingSaveIsSvg: Boolean = false

        private fun requestCode(timestamp: Long, seed: Int): Int = timestamp.toInt() + seed

        fun scheduleAlarm(context: Context, timestamp: Long, planetName: String) {
            val pi = PendingIntent.getBroadcast(context, requestCode(timestamp, ALARM_SEED),
                Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("planetName", planetName); putExtra("timestamp", timestamp)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            scheduleExact(context, timestamp, pi, "Alarm", "$planetName @ $timestamp")
        }

        fun scheduleReminder(context: Context, timestamp: Long, planetName: String, leadMinutes: Long) {
            val t = timestamp - leadMinutes * 60_000
            if (t <= System.currentTimeMillis()) return
            val pi = PendingIntent.getBroadcast(context, requestCode(timestamp, ALARM_SEED + 1),
                Intent(context, ReminderReceiver::class.java).apply {
                    action = REMINDER_ACTION; putExtra("planetName", planetName)
                    putExtra("timestamp", timestamp); putExtra("leadMinutes", leadMinutes)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            scheduleExact(context, t, pi, "Reminder", "$planetName ${leadMinutes}m before @ $t")
        }

        private fun scheduleExact(context: Context, fireAt: Long, pi: PendingIntent, label: String, detail: String) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
                else am.setExact(AlarmManager.RTC_WAKEUP, fireAt, pi)
                Log.d(TAG, "$label: $detail")
            } catch (e: SecurityException) {
                // SCHEDULE_EXACT_ALARM not granted — don't silently drop it; degrade
                // to an inexact wakeup alarm (still fires, may be late in Doze).
                Log.w(TAG, "$label inexact (no exact-alarm permission): $detail")
                am.set(AlarmManager.RTC_WAKEUP, fireAt, pi)
            }
        }

        fun cancelAlarm(context: Context, timestamp: Long) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(PendingIntent.getBroadcast(context, requestCode(timestamp, ALARM_SEED),
                Intent(context, AlarmReceiver::class.java).apply { putExtra("timestamp", timestamp) },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            am.cancel(PendingIntent.getBroadcast(context, requestCode(timestamp, ALARM_SEED + 1),
                Intent(context, ReminderReceiver::class.java).apply { action = REMINDER_ACTION; putExtra("timestamp", timestamp) },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.cancel(timestamp.toInt())
            nm.cancel(timestamp.toInt() + 1)
        }

        /** Re-register all persisted future alarms. Used by BootReceiver after reboot/timezone/app-replaced. */
        @JvmStatic
        fun scheduleFromStore(context: Context) {
            val now = System.currentTimeMillis()
            AlarmStore.load(context).forEach { a ->
                if (a.timestamp < now) {
                    AlarmStore.remove(context, a.timestamp)
                } else {
                    scheduleAlarm(context, a.timestamp, a.planet)
                    if (a.leadMinutes > 0) scheduleReminder(context, a.timestamp, a.planet, a.leadMinutes.toLong())
                }
            }
        }

        /** Write bytes to a user-visible location; returns the absolute/MediaStore path string. */
        fun writeToUserStorage(context: Context, bytes: ByteArray, mime: String, fileName: String, isSvg: Boolean): String {
            val res = context.resources
            val appName = try { res.getString(res.getIdentifier("app_name", "string", context.packageName)) } catch (e: Exception) { "PlanetarySigils" }
            val dirName = appName.replace(" ", "")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Images -> Pictures (visible in Gallery); SVG -> Download (Gallery can't render SVG).
                val collection = if (isSvg) MediaStore.Downloads.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val relativeDir = if (isSvg) "${Environment.DIRECTORY_DOWNLOADS}/$dirName" else "${Environment.DIRECTORY_PICTURES}/$dirName"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(collection, values) ?: throw IllegalStateException("MediaStore insert failed")
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: throw IllegalStateException("MediaStore output failed")
                return uri.toString()
            }

            // Legacy API 22-28: public external storage. Images -> Pictures so the
            // gallery picks them up; SVG -> Downloads. WRITE_EXTERNAL_STORAGE is
            // declared with maxSdkVersion=28 (no prompt on API 29+).
            val type = if (isSvg) Environment.DIRECTORY_DOWNLOADS else Environment.DIRECTORY_PICTURES
            val base = Environment.getExternalStoragePublicDirectory(type)
            val dir = File(base, dirName)
            if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("Could not create $dir")
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mime), null)
            return file.absolutePath
        }
    }
}
