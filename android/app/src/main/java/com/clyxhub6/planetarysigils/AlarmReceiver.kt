package com.clyxhub6.planetarysigils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val planetName = intent.getStringExtra("planetName") ?: "Unknown"
        val timestamp = intent.getLongExtra("timestamp", 0L)
        Log.d("AlarmReceiver", "Alarm fired for planet: $planetName at $timestamp")

        // Launch the full-screen alarm activity (it plays the ringtone itself).
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
    }
}
