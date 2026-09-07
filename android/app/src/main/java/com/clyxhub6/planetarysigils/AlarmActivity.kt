package com.clyxhub6.planetarysigils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AlarmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_alarm)

        val planetName = intent.getStringExtra("planetName") ?: "Unknown"
        findViewById<TextView>(R.id.planetTitle).text = "$planetName Hour"

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            stopAlarmService()
            finish()
        }

        findViewById<Button>(R.id.btnSnooze).setOnClickListener {
            stopAlarmService()
            val snoozeMs = System.currentTimeMillis() + (5 * 60 * 1000)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val receiverIntent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("planetName", planetName)
                putExtra("timestamp", snoozeMs)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                snoozeMs.toInt(),
                receiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeMs, pendingIntent)
            }
            finish()
        }
    }

    private fun stopAlarmService() {
        val serviceIntent = Intent(this, AlarmService::class.java)
        stopService(serviceIntent)
    }
}
