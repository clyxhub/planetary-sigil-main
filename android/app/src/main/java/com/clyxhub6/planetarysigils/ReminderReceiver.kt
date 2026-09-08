package com.clyxhub6.planetarysigils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val planetName = intent.getStringExtra("planetName") ?: "Unknown"
        val timestamp = intent.getLongExtra("timestamp", 0L)
        val leadMinutes = intent.getLongExtra("leadMinutes", 0L)
        Log.d(TAG, "Reminder fired: $planetName ${leadMinutes}m before @ $timestamp")

        val contentIntent = PendingIntent.getActivity(
            context, timestamp.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val leadLabel = if (leadMinutes >= 60) "${leadMinutes / 60}h" else "${leadMinutes}min"

        val notification = NotificationCompat.Builder(context, PlanetaryAlarmPlugin.REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Coming up: $planetName hour")
            .setContentText("Begins in $leadLabel. Prepare your sigil.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(timestamp.toInt(), notification)
    }

    companion object {
        private const val TAG = "ReminderReceiver"
    }
}
