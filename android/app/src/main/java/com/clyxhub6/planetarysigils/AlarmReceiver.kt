package com.clyxhub6.planetarysigils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Fires when an AlarmManager alarm for a planetary hour is delivered, and also
 * handles the Dismiss / Snooze actions on the alarm notification.
 *
 * Reliability design:
 *  1. A single high-importance notification carrying Snooze/Dismiss actions and
 *     a full-screen intent is posted. When the screen is off the full-screen
 *     intent opens AlarmActivity; when the phone is in use the OS shows the
 *     same notification as a heads-up banner with the actions available.
 *  2. AlarmService is started as a foreground service so the alarm stays
 *     audible when the app is closed, and it reuses the same notification so
 *     the heads-up always has working actions.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val planetName = intent.getStringExtra("planetName") ?: "Unknown"
        val timestamp = intent.getLongExtra("timestamp", 0L)

        when (intent.action) {
            ACTION_DISMISS -> {
                Log.d(TAG, "Dismiss @ $timestamp")
                stopAlarmOutput(context)
                return
            }
            ACTION_SNOOZE -> {
                Log.d(TAG, "Snooze @ $timestamp")
                stopAlarmOutput(context)
                scheduleSnooze(context, planetName, timestamp)
                return
            }
        }

        Log.d(TAG, "Alarm fired: $planetName @ $timestamp")
        fireAlarm(context, planetName, timestamp)
    }

    private fun fireAlarm(context: Context, planetName: String, timestamp: Long) {
        AlarmNotifications.ensureChannels(context)

        // Post the alarm notification first so it appears (and heads-up shows)
        // even if starting the foreground service is delayed.
        val notification = AlarmNotifications.buildAlarmNotification(context, planetName, timestamp)
        context.getSystemService(NotificationManager::class.java)
            .notify(AlarmNotifications.ALARM_NOTIFICATION_ID, notification)

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

    private fun stopAlarmOutput(context: Context) {
        try { context.stopService(Intent(context, AlarmService::class.java)) } catch (_: Exception) {}
        context.getSystemService(NotificationManager::class.java)
            .cancel(AlarmNotifications.ALARM_NOTIFICATION_ID)
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
