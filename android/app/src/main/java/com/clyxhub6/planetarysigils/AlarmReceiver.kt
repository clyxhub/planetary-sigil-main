package com.clyxhub6.planetarysigils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val planetName = intent.getStringExtra("planetName") ?: "Unknown"
        val timestamp = intent.getLongExtra("timestamp", 0L)
        Log.d(TAG, "Alarm fired: $planetName @ $timestamp")

        // 1. Launch the full-screen alarm activity (plays ringtone + vibrates).
        try {
            val activityIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtra("planetName", planetName)
                putExtra("timestamp", timestamp)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            context.startActivity(activityIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not launch AlarmActivity", e)
        }

        // 2. Also post a full-screen notification as a backup. If the activity
        //    launch is blocked (some OEMs restrict background activity starts),
        //    the notification ensures the user still sees and hears the alarm.
        postAlarmNotification(context, planetName, timestamp)
    }

    private fun postAlarmNotification(context: Context, planetName: String, timestamp: Long) {
        val nm = context.getSystemService(NotificationManager::class.java)

        // Tap opens the main app
        val contentIntent = PendingIntent.getActivity(
            context, timestamp.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action button to dismiss
        val dismissIntent = PendingIntent.getBroadcast(
            context, timestamp.toInt() + 100,
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra("planetName", "")
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
            .setFullScreenIntent(contentIntent, true)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        nm.notify(timestamp.toInt(), notification)
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
