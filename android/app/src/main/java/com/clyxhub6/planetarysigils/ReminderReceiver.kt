package com.clyxhub6.planetarysigils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Fires at (planetaryHourStart - leadMinutes) to deliver the early "hour is
 * coming up" reminder. The OS alarm is scheduled by PlanetaryAlarmPlugin and
 * re-registered after boot by BootReceiver; this receiver only constructs and
 * posts the notification.
 *
 * A persisted marker (see AlarmStore.isReminderFired) prevents the reminder
 * from being posted twice when the device restarts between scheduling and firing.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val planetName = intent.getStringExtra("planetName") ?: "Unknown"
        val timestamp = intent.getLongExtra("timestamp", 0L)
        val leadMinutes = intent.getLongExtra("leadMinutes", 0L)
        Log.d(TAG, "Reminder fired: $planetName ${leadMinutes}m before @ $timestamp")

        if (timestamp <= 0L || AlarmStore.isReminderFired(context, timestamp)) {
            Log.d(TAG, "Skipping duplicate reminder @ $timestamp")
            return
        }
        AlarmStore.markReminderFired(context, timestamp)

        val contentIntent = PendingIntent.getActivity(
            context, timestamp.toInt(),
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
            .notify(timestamp.toInt() + 1, notification)
    }

    companion object {
        private const val TAG = "ReminderReceiver"
    }
}
