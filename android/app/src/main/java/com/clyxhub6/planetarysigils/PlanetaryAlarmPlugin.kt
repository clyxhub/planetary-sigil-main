package com.clyxhub6.planetarysigils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
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

@CapacitorPlugin(
    name = "PlanetaryAlarm",
    permissions = [
        Permission(alias = "notifications", strings = [Manifest.permission.POST_NOTIFICATIONS])
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

    // ── Notification Permission ──────────────────────────────────────────

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
            if (!granted) { activity?.let { ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2002) } }
            val ret = JSObject(); ret.put("value", granted); call.resolve(ret)
        } else {
            val ret = JSObject(); ret.put("value", true); call.resolve(ret)
        }
    }

    // ── Exact Alarm Permission ───────────────────────────────────────────

    @PluginMethod
    fun hasExactAlarmPermission(call: PluginCall) {
        val ret = JSObject()
        ret.put("value", Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager().canScheduleExactAlarms())
        call.resolve(ret)
    }

    @PluginMethod
    fun requestExactAlarmPermission(call: PluginCall) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager().canScheduleExactAlarms()) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
        val ret = JSObject(); ret.put("value", true); call.resolve(ret)
    }

    // ── Schedule / Cancel ────────────────────────────────────────────────

    @PluginMethod
    fun schedule(call: PluginCall) {
        val timestamp = call.getLong("timestamp")?.toLong() ?: call.getString("timestamp")?.toLongOrNull()
        val planetName = call.getString("planetName")
        val leadMinutes = call.getInt("leadMinutes") ?: 0
        if (timestamp == null || planetName == null) { call.reject("Must provide timestamp and planetName"); return }

        scheduleAlarm(timestamp, planetName)
        if (leadMinutes > 0) scheduleReminder(timestamp, planetName, leadMinutes.toLong())

        val ret = JSObject(); ret.put("success", true); call.resolve(ret)
    }

    @PluginMethod
    fun cancel(call: PluginCall) {
        val timestamp = call.getLong("timestamp")?.toLong() ?: call.getString("timestamp")?.toLongOrNull() ?: return
        val am = alarmManager()

        am.cancel(PendingIntent.getBroadcast(context, timestamp.toInt(),
            Intent(context, AlarmReceiver::class.java).apply { putExtra("timestamp", timestamp) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        am.cancel(PendingIntent.getBroadcast(context, timestamp.toInt() + 1,
            Intent(context, ReminderReceiver::class.java).apply { action = REMINDER_ACTION; putExtra("timestamp", timestamp) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        context.getSystemService(NotificationManager::class.java).cancel(timestamp.toInt())

        val ret = JSObject(); ret.put("success", true); call.resolve(ret)
    }

    private fun scheduleAlarm(timestamp: Long, planetName: String) {
        val pi = PendingIntent.getBroadcast(context, timestamp.toInt(),
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra("planetName", planetName); putExtra("timestamp", timestamp)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) alarmManager().setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pi)
            else alarmManager().setExact(AlarmManager.RTC_WAKEUP, timestamp, pi)
            Log.d(TAG, "Alarm: $planetName @ $timestamp")
        } catch (e: SecurityException) { alarmManager().set(AlarmManager.RTC_WAKEUP, timestamp, pi) }
    }

    private fun scheduleReminder(timestamp: Long, planetName: String, leadMinutes: Long) {
        val t = timestamp - leadMinutes * 60_000
        if (t <= System.currentTimeMillis()) return
        val pi = PendingIntent.getBroadcast(context, timestamp.toInt() + 1,
            Intent(context, ReminderReceiver::class.java).apply {
                action = REMINDER_ACTION; putExtra("planetName", planetName)
                putExtra("timestamp", timestamp); putExtra("leadMinutes", leadMinutes)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) alarmManager().setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
            else alarmManager().setExact(AlarmManager.RTC_WAKEUP, t, pi)
            Log.d(TAG, "Reminder: $planetName ${leadMinutes}m before @ $t")
        } catch (e: SecurityException) { alarmManager().set(AlarmManager.RTC_WAKEUP, t, pi) }
    }

    // ── Download: write to app dir + scan into MediaStore ────────────────
    // This approach uses NO activity results, NO MediaStore.insert, and
    // requires zero permissions. It writes to the app's external files dir
    // (guaranteed writable on every Android version), then tells the media
    // scanner about it so it appears in the file manager / gallery.

    @PluginMethod
    fun saveFile(call: PluginCall) {
        val dataUrl = call.getString("dataUrl")
        val svg = call.getString("svg")
        val fileName = call.getString("fileName") ?: "sigil.png"
        if (dataUrl == null && svg == null) { call.reject("dataUrl or svg required"); return }

        val mime = when {
            fileName.endsWith("svg", true) -> "image/svg+xml"
            fileName.endsWith("jpg", true) || fileName.endsWith("jpeg", true) -> "image/jpeg"
            else -> "image/png"
        }
        val bytes = if (svg != null) svg.toByteArray(Charsets.UTF_8) else {
            Base64.decode(dataUrl!!.substringAfter(","), Base64.DEFAULT)
        }
        if (bytes.isEmpty()) { call.reject("Could not decode data"); return }

        try {
            // Write to our own external dir — always works, no permissions
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(bytes) }

            // Scan so the file shows up in the system file manager
            android.media.MediaScannerConnection.scanFile(
                context, arrayOf(file.absolutePath), arrayOf(mime), null
            )

            val ret = JSObject()
            ret.put("success", true)
            ret.put("path", file.absolutePath)
            call.resolve(ret)
            Log.d(TAG, "Saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "saveFile failed", e)
            call.reject(e.message ?: "Could not save file")
        }
    }

    private fun alarmManager() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "PlanetaryAlarm"
        const val ALARM_CHANNEL_ID = "planetary_alarm_channel"
        const val REMINDER_CHANNEL_ID = "planetary_reminder_channel"
        const val REMINDER_ACTION = "com.clyxhub6.planetarysigils.REMINDER"
    }
}
