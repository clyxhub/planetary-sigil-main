package com.clyxhub6.planetarysigils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Fires when an AlarmManager alarm for a planetary hour is delivered.
 *
 * Reliability design (works when app is backgrounded, screen locked, or process
 * killed, and under Doze):
 *
 *  1. A notification is posted on the high-importance ALARM_CHANNEL_ID with a
 *     full-screen intent that launches AlarmActivity. Android treats
 *     full-screen-intent alarms from a BroadcastReceiver as a sanctioned
 *     background path to show a full-screen activity that wakes the screen and
 *     plays sound — this no longer relies on an unrestricted Activity start
 *     (which Android 10+ blocks from background receivers).
 *  2. AlarmService is started as a foreground service so audible/vibrating alarm
 *     output is guaranteed even if some OEM restricts the full-screen activity.
 *
 * The channel is created in PlanetaryAlarmPlugin.createNotificationChannels()
 * before any alarm is scheduled, so it is always valid at delivery time.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val planetName = intent.getStringExtra("planetName") ?: "Unknown"
        val timestamp = intent.getLongExtra("timestamp", 0L)
        Log.d(TAG, "Alarm fired: $planetName @ $timestamp")

        // Primary: full-screen intent so the alarm shows and plays on the lock
        // screen / with the app closed.
        val fsi = PendingIntent.getActivity(
            context, timestamp.toInt(),
            Intent(context, AlarmActivity::class.java).apply {
                putExtra("planetName", planetName)
                putExtra("timestamp", timestamp)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Fallback tap target: open the web UI.
        val contentIntent = PendingIntent.getActivity(
            context, timestamp.toInt() + 50,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PlanetaryAlarmPlugin.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Planetary Hour: $planetName")
            .setContentText("The hour of $planetName has begun.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fsi, true)
            .setContentIntent(fsi)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(timestamp.toInt(), notification)

        // Guaranteed audible/vibrating foreground backup.
        try {
            val svc = Intent(context, AlarmService::class.java).apply {
                putExtra("planetName", planetName)
                putExtra("timestamp", timestamp)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc)
            } else {
                context.startService(svc)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not start AlarmService", e)
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
