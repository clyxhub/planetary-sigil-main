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
 * Fires when an AlarmManager alarm for a planetary hour is delivered, and also
 * handles the Dismiss / Snooze actions on the alarm notification.
 *
 * Reliability design:
 *  1. A high-importance notification is posted with a full-screen intent that
 *     opens AlarmActivity (wakes the screen, plays sound, gives Dismiss/Snooze).
 *  2. AlarmService is started as a foreground service so the alarm is audible
 *     even when the app is closed or the full-screen activity is blocked.
 *  3. The notification itself carries Dismiss and Snooze actions, so the user
 *     can always stop the alarm from the lock screen without opening the app.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val planetName = intent.getStringExtra("planetName") ?: "Unknown"
        val timestamp = intent.getLongExtra("timestamp", 0L)

        when (intent.action) {
            ACTION_DISMISS -> {
                Log.d(TAG, "Dismiss @ $timestamp")
                stopAlarmOutput(context, timestamp)
                return
            }
            ACTION_SNOOZE -> {
                Log.d(TAG, "Snooze @ $timestamp")
                stopAlarmOutput(context, timestamp)
                scheduleSnooze(context, planetName, timestamp)
                return
            }
        }

        Log.d(TAG, "Alarm fired: $planetName @ $timestamp")
        fireAlarm(context, planetName, timestamp)
    }

    private fun fireAlarm(context: Context, planetName: String, timestamp: Long) {
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

        val dismissIntent = PendingIntent.getBroadcast(
            context, timestamp.toInt() + 101,
            Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra("timestamp", timestamp)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context, timestamp.toInt() + 102,
            Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra("planetName", planetName)
                putExtra("timestamp", timestamp)
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
            .addAction(0, "Dismiss", dismissIntent)
            .addAction(0, "Snooze 5 min", snoozeIntent)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(timestamp.toInt(), notification)

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

    private fun stopAlarmOutput(context: Context, timestamp: Long) {
        try { context.stopService(Intent(context, AlarmService::class.java)) } catch (_: Exception) {}
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(timestamp.toInt())
        nm.cancel(timestamp.toInt() + 1)
    }

    private fun scheduleSnooze(context: Context, planetName: String, timestamp: Long) {
        val snoozeAt = System.currentTimeMillis() + 5 * 60_000L
        PlanetaryAlarmPlugin.scheduleAlarm(context, snoozeAt, planetName)
        Log.d(TAG, "Snoozed $planetName until $snoozeAt")
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_DISMISS = "com.clyxhub6.planetarysigils.ALARM_DISMISS"
        const val ACTION_SNOOZE = "com.clyxhub6.planetarysigils.ALARM_SNOOZE"
    }
}
