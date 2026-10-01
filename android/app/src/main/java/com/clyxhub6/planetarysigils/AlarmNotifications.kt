package com.clyxhub6.planetarysigils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Central place for the planetary-hour alarm notification and its channels.
 *
 * The alarm is a SINGLE high-importance notification carrying Snooze and Dismiss
 * actions. It is posted by AlarmReceiver (so it appears even if the foreground
 * service is slow) and reused by AlarmService.startForeground, so the very same
 * notification — buttons included — is what the OS shows as a heads-up banner
 * while the phone is in use. A full-screen intent still opens AlarmActivity when
 * the screen is off.
 */
object AlarmNotifications {

    // Fixed id so the notification and its actions always refer to one slot
    // (millisecond timestamps overflow Int and are awkward to cancel).
    const val ALARM_NOTIFICATION_ID = 9101

    private const val REQ_FSI = 9201
    private const val REQ_DISMISS = 9202
    private const val REQ_SNOOZE = 9203

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                PlanetaryAlarmPlugin.ALARM_CHANNEL_ID,
                "Planetary Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up alarms for planetary hours"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                // Audio is played by AlarmService / AlarmActivity, so keep the
                // channel silent and avoid double ringing.
                setSound(null, null)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                PlanetaryAlarmPlugin.REMINDER_CHANNEL_ID,
                "Planetary Hour Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up notification before a planetary hour"
                enableVibration(true)
            }
        )
    }

    fun buildAlarmNotification(context: Context, planetName: String, timestamp: Long): Notification {
        val fsi = PendingIntent.getActivity(
            context, REQ_FSI,
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
            context, REQ_DISMISS,
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_DISMISS
                putExtra("planetName", planetName)
                putExtra("timestamp", timestamp)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context, REQ_SNOOZE,
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_SNOOZE
                putExtra("planetName", planetName)
                putExtra("timestamp", timestamp)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, PlanetaryAlarmPlugin.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Planetary Hour: $planetName")
            .setContentText("The hour of $planetName has begun.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fsi, true)
            .setContentIntent(fsi)
            .setAutoCancel(false)
            .setOngoing(true)
            .setShowWhen(true)
            .addAction(0, "Snooze 5 min", snoozeIntent)
            .addAction(0, "Dismiss", dismissIntent)
            .build()
    }
}
