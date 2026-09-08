package com.clyxhub6.planetarysigils

import android.Manifest
import android.app.AlarmManager
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
import androidx.core.app.ActivityCompat
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

    @PluginMethod
    fun schedule(call: PluginCall) {
        // The alarm ring is NOT gated on the notification permission: it always
        // gets scheduled, and rings even if the user declined POST_NOTIFICATIONS.
        // (POST_NOTIFICATIONS only controls whether the reminder/notification is
        // visible, not whether the exact alarm fires.)
        val timestamp = call.getLong("timestamp")?.toLong()
            ?: call.getString("timestamp")?.toLongOrNull()
        val planetName = call.getString("planetName")
        val leadMinutes = call.getInt("leadMinutes") ?: 0

        if (timestamp == null || planetName == null) {
            call.reject("Must provide timestamp and planetName")
            return
        }

        scheduleAlarm(timestamp, planetName)
        if (leadMinutes > 0) {
            scheduleReminder(timestamp, planetName, leadMinutes.toLong())
        }

        val ret = JSObject()
        ret.put("success", true)
        call.resolve(ret)
    }

    @PluginMethod
    fun hasNotificationPermission(call: PluginCall) {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val ret = JSObject()
        ret.put("value", granted)
        call.resolve(ret)
    }

    @PluginMethod
    fun requestNotificationPermission(call: PluginCall) {
        val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                val activity = activity
                if (activity != null) {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        2002
                    )
                }
            }
            granted
        } else {
            true
        }
        val ret = JSObject()
        ret.put("value", ok)
        call.resolve(ret)
    }

    @PluginMethod
    fun saveMedia(call: PluginCall) {
        val dataUrl = call.getString("dataUrl")
        val fileName = call.getString("fileName") ?: "sigil.png"
        if (dataUrl == null) {
            call.reject("dataUrl required")
            return
        }
        val base64 = dataUrl.substringAfter(",")
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val mime = if (fileName.endsWith("jpg") || fileName.endsWith("jpeg")) "image/jpeg" else "image/png"
        writeOut(bytes, fileName, mime, call)
    }

    @PluginMethod
    fun saveSvg(call: PluginCall) {
        val svg = call.getString("svg")
        val fileName = call.getString("fileName") ?: "sigil.svg"
        if (svg == null) {
            call.reject("svg required")
            return
        }
        val bytes = svg.toByteArray()
        writeOut(bytes, fileName, "image/svg+xml", call)
    }

    // Write the file to the app's cache dir (works on ANY Android version, zero
    // permissions) and open the Android system file/share sheet via FileProvider.
    // The user picks their Downloads app / file manager and the system saves it.
    @PluginMethod
    fun openFileWithSystemUI(call: PluginCall) {
        val dataUrl = call.getString("dataUrl")
        val svg = call.getString("svg")
        val fileName = call.getString("fileName") ?: "sigil.png"
        if (dataUrl == null && svg == null) {
            call.reject("dataUrl or svg required")
            return
        }
        val mime = when {
            fileName.endsWith("svg", true) -> "image/svg+xml"
            fileName.endsWith("jpg", true) || fileName.endsWith("jpeg", true) -> "image/jpeg"
            else -> "image/png"
        }
        val bytes = if (svg != null) svg.toByteArray() else {
            Base64.decode(dataUrl!!.substringAfter(","), Base64.DEFAULT)
        }
        if (bytes.isEmpty()) {
            call.reject("Could not decode data")
            return
        }

        try {
            val dir = context.cacheDir
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(bytes) }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            val ret = JSObject()
            ret.put("success", true)
            ret.put("where", "system")
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e("PlanetaryAlarm", "openFileWithSystemUI failed", e)
            call.reject(e.message ?: "Could not open file")
        }
    }

    // Resilient save: tries MediaStore Downloads (shared, discoverable), then an
    // app-external directory as a fallback. Only rejects if nothing could be written.
    private fun writeOut(bytes: ByteArray, fileName: String, mime: String, call: PluginCall) {
        val context = context
        var wrote = false
        var where = "Downloads/PlanetarySigils"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                // Strategy A: MediaStore Downloads with a friendly subfolder.
                for (relPath in listOf("Download/PlanetarySigils", "Download")) {
                    try {
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, mime)
                            put(MediaStore.Downloads.RELATIVE_PATH, relPath)
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        if (uri != null) {
                            val os = resolver.openOutputStream(uri)
                            os?.write(bytes)
                            os?.close()
                            values.clear()
                            values.put(MediaStore.Downloads.IS_PENDING, 0)
                            resolver.update(uri, values, null, null)
                            wrote = true
                            where = if (relPath.contains("PlanetarySigils")) "Downloads/PlanetarySigils" else "Downloads"
                            break
                        }
                    } catch (e: Exception) {
                        Log.w("PlanetaryAlarm", "MediaStore save to '$relPath' failed", e)
                    }
                }
            }

            // Strategy B: app-external files dir (no permission needed) + index it.
            if (!wrote) {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val file = File(dir, fileName)
                try {
                    file.writeBytes(bytes)
                    try {
                        android.media.MediaScannerConnection.scanFile(
                            context, arrayOf(file.absolutePath), arrayOf(mime), null
                        )
                    } catch (se: Exception) { Log.w("PlanetaryAlarm", "scan failed", se) }
                    wrote = true
                    where = "app files/Downloads"
                } catch (e: Exception) {
                    Log.e("PlanetaryAlarm", "App-dir save failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PlanetaryAlarm", "writeOut failed", e)
        }

        val ret = JSObject()
        if (wrote) {
            ret.put("success", true)
            ret.put("where", where)
            call.resolve(ret)
        } else {
            call.reject("Could not write the file")
        }
    }

    @PluginMethod
    fun cancel(call: PluginCall) {
        val timestamp = call.getLong("timestamp")?.toLong()
            ?: call.getString("timestamp")?.toLongOrNull()

        if (timestamp == null) {
            call.reject("Must provide timestamp")
            return
        }

        val context = context
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel the ringing alarm
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("planetName", "")
            putExtra("timestamp", timestamp)
        }
        val alarmPi = PendingIntent.getBroadcast(
            context,
            timestamp.toInt(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(alarmPi)

        // Cancel any scheduled reminder notification
        val reminderIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = Constants.REMINDER_ACTION
            putExtra("timestamp", timestamp)
        }
        val reminderPi = PendingIntent.getBroadcast(
            context,
            timestamp.toInt(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(reminderPi)

        // Dismiss any already-posted notification for this hour
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(timestamp.toInt())

        val ret = JSObject()
        ret.put("success", true)
        call.resolve(ret)
    }

    @PluginMethod
    fun hasExactAlarmPermission(call: PluginCall) {
        val ret = JSObject()
        ret.put("value", Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager().canScheduleExactAlarms())
        call.resolve(ret)
    }

    @PluginMethod
    fun requestExactAlarmPermission(call: PluginCall) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager().canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        call.resolve()
    }

    private fun alarmManager() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun scheduleAlarm(timestamp: Long, planetName: String) {
        val context = context
        val alarmManager = alarmManager()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("planetName", planetName)
            putExtra("timestamp", timestamp)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            timestamp.toInt(), 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent)
            }
            Log.d("PlanetaryAlarm", "Scheduled exact alarm for $planetName at $timestamp")
        } catch (e: SecurityException) {
            Log.e("PlanetaryAlarm", "Exact alarm permission missing", e)
            // Fallback for Android 14+ if permission is denied
            alarmManager.set(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent)
        }
    }

    private fun scheduleReminder(timestamp: Long, planetName: String, leadMinutes: Long) {
        val reminderTime = timestamp - (leadMinutes * 60 * 1000)
        if (reminderTime <= System.currentTimeMillis()) return // already passed

        val context = context
        val alarmManager = alarmManager()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = Constants.REMINDER_ACTION
            putExtra("planetName", planetName)
            putExtra("timestamp", timestamp)
            putExtra("leadMinutes", leadMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            timestamp.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            }
            Log.d("PlanetaryAlarm", "Scheduled reminder for $planetName ${leadMinutes}min before $timestamp")
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
        }
    }
}

object Constants {
    const val REMINDER_ACTION = "com.clyxhub6.planetarysigils.REMINDER"
    const val REMINDER_CHANNEL_ID = "planetary_reminder_channel"
}
