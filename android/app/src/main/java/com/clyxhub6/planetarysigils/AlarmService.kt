package com.clyxhub6.planetarysigils

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

/**
 * Foreground service that provides guaranteed audible + vibrating alarm output.
 *
 * It reuses the single alarm notification built by AlarmNotifications (with its
 * Snooze/Dismiss actions) via startForeground, so the heads-up shown while the
 * phone is in use always carries working buttons rather than a separate,
 * action-less foreground notification.
 */
class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    // Requirement 3 failsafe: if the user cannot reach the alarm UI (e.g. the
    // alarm screen failed to show), the alarm must still stop by itself. The
    // service force-stops after MAX_RING_MS so it can never ring forever.
    private val autoStop = object : Runnable {
        override fun run() {
            Log.w(TAG, "Alarm auto-stopped after ${MAX_RING_MS / 60000} minutes")
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRinging = true
        handler.postDelayed(autoStop, MAX_RING_MS)
        val planetName = intent?.getStringExtra("planetName") ?: "Unknown"
        val timestamp = intent?.getLongExtra("timestamp", 0L) ?: 0L

        AlarmNotifications.ensureChannels(this)

        // Reuse the alarm notification so the heads-up (and its actions) is the
        // same one the receiver posted.
        try {
            startForeground(
                AlarmNotifications.ALARM_NOTIFICATION_ID,
                AlarmNotifications.buildAlarmNotification(this, planetName, timestamp)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Could not post foreground alarm notification", e)
        }

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
        isRinging = false
        handler.removeCallbacks(autoStop)
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
        private const val MAX_RING_MS = 10 * 60_000L
        private val handler = android.os.Handler(android.os.Looper.getMainLooper())
        @Volatile var isRinging: Boolean = false
            private set
        /** Force-stop the ringing alarm from anywhere (kill switch). */
        fun stopRing(context: Context) {
            try { context.stopService(Intent(context, AlarmService::class.java)) } catch (_: Exception) {}
        }
    }
}
