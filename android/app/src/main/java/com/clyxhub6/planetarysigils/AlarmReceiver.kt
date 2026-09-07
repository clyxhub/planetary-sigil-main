package com.clyxhub6.planetarysigils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val planetName = intent.getStringExtra("planetName") ?: "Unknown"
        val timestamp = intent.getLongExtra("timestamp", 0L)
        Log.d("AlarmReceiver", "Alarm fired for planet: $planetName at $timestamp")

        // 1. Enqueue the task to Foreground Service to play the alarm sound continuously
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("planetName", planetName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // 2. Launch Full Screen Activity to show Snooze/Dismiss UI over lock screen
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("planetName", planetName)
            putExtra("timestamp", timestamp)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(activityIntent)
    }
}
