package com.clyxhub6.planetarysigils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

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

        startAlarmSound()

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            stopAlarm()
            finish()
        }

        findViewById<Button>(R.id.btnSnooze).setOnClickListener {
            stopAlarm()
            snooze(planetName)
            finish()
        }
    }

    private fun startAlarmSound() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmActivity, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Could not start alarm sound", e)
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        // Stop any leftover foreground service (kept for compatibility).
        try { stopService(Intent(this, AlarmService::class.java)) } catch (e: Exception) { }
    }

    private fun snooze(planetName: String) {
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeMs, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeMs, pendingIntent)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
