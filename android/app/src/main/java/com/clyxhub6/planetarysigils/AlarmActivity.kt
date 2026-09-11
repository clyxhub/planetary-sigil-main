package com.clyxhub6.planetarysigils

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
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

        // Show on lock screen and wake the display
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
        val timestamp = intent.getLongExtra("timestamp", 0L)

        findViewById<TextView>(R.id.planetTitle).text = "$planetName Hour"

        // Dismiss the backup notification that AlarmReceiver posted
        getSystemService(NotificationManager::class.java).cancel(timestamp.toInt())

        // The full-screen activity now owns the audio; stop the foreground
        // backup service so the alarm does not play twice.
        try { stopService(Intent(this, AlarmService::class.java)) } catch (_: Exception) {}

        // Start ringtone + vibration
        startAlarmSound()

        // Dismiss button
        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            stopAlarm()
            finish()
        }

        // Snooze button (5 min)
        findViewById<Button>(R.id.btnSnooze).setOnClickListener {
            stopAlarm()
            snooze(planetName)
            finish()
        }
    }

    private fun startAlarmSound() {
        try {
            val chosen = AlarmStore.getAlarmSoundUri(this)?.let { Uri.parse(it) }
            val ringtoneUri = chosen
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

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
                val mgr = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                mgr.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
        } catch (e: Exception) {
            Log.e(TAG, "Could not start alarm sound", e)
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        vibrator?.cancel()
    }

    private fun snooze(planetName: String) {
        val snoozeAt = System.currentTimeMillis() + 5 * 60_000L
        val pi = PendingIntent.getBroadcast(
            this, snoozeAt.toInt(),
            Intent(this, AlarmReceiver::class.java).apply {
                putExtra("planetName", planetName)
                putExtra("timestamp", snoozeAt)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeAt, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, snoozeAt, pi)
            }
        } catch (e: SecurityException) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.set(AlarmManager.RTC_WAKEUP, snoozeAt, pi)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AlarmActivity"
    }
}
