package com.clyxhub6.planetarysigils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Foreground service that provides guaranteed audible + vibrating alarm output.
 *
 * The AlarmReceiver starts this service after posting the full-screen
 * notification. Because it is foreground, the audio keeps playing even if the
 * app process was killed, so the alarm cannot be silently dropped. The channel
 * is deliberately the same ALARM_CHANNEL_ID created by PlanetaryAlarmPlugin so
 * there is exactly one high-importance alarm channel.
 */
class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val planetName = intent?.getStringExtra("planetName") ?: "Unknown"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    PlanetaryAlarmPlugin.ALARM_CHANNEL_ID,
                    "Planetary Alarms",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val notification: Notification = NotificationCompat.Builder(this, PlanetaryAlarmPlugin.ALARM_CHANNEL_ID)
            .setContentTitle("Planetary Hour: $planetName")
            .setContentText("The hour of $planetName has begun.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)

        // Play the chosen alarm sound (looping) at alarm volume; fall back to the
        // system default alarm tone.
        try {
            val chosen = AlarmStore.getAlarmSoundUri(this)?.let { Uri.parse(it) }
            val alarmUri = chosen
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, alarmUri)
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
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
        } catch (e: Exception) {
            Log.e(TAG, "Could not start alarm sound", e)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            vibrator?.cancel()
        } catch (_: Exception) {}
        mediaPlayer = null
        vibrator = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AlarmService"
    }
}
