package com.prayerlink.app.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.prayerlink.app.MainActivity
import com.prayerlink.app.R
import com.prayerlink.app.data.datasource.PreferencesKeys
import com.prayerlink.app.data.datasource.prayerLinkDataStore
import com.prayerlink.app.data.model.NotificationSoundMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * A Foreground Service that handles playing the Adhan (or other sounds) 
 * reliably in the background, showing a high-priority notification with Media Controls.
 */
class PrayerAdhanService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_DISMISS = "ACTION_DISMISS"
        const val EXTRA_PRAYER_NAME = "EXTRA_PRAYER_NAME"
        private const val NOTIFICATION_ID = 2001
        private const val PLAYBACK_DURATION_MS = 12_000L
    }

    private var mediaPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var playbackJob: Job? = null
    private var audioManager: android.media.AudioManager? = null
    private var focusRequest: Any? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
                startForegroundNotification(prayerName)
                serviceScope.launch {
                    scheduleInitialReminder(prayerName)
                    playAudio()
                }
            }
            ACTION_STOP -> {
                stopPlaybackAndService(removeNotification = false)
            }
            ACTION_DISMISS -> {
                stopPlaybackAndService(removeNotification = true)
            }
        }
        return START_NOT_STICKY
    }

    private fun scheduleInitialReminder(prayerName: String) {
        val prayer = runCatching { com.prayerlink.app.data.model.Prayer.valueOf(prayerName) }.getOrNull()
        if (prayer != null) {
            serviceScope.launch {
                val scheduler = PrayerReminderScheduler(applicationContext)
                scheduler.scheduleRemindersForPrayer(prayer)
            }
        }
    }

    private fun startForegroundNotification(prayerName: String) {
        // Ensure channel exists
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = nm.getNotificationChannel(PrayerNotificationManager.CHANNEL_ID)
            if (channel == null) {
                Log.w("PrayerAdhanService", "Notification channel missing. Recreating it.")
                channel = android.app.NotificationChannel(
                    PrayerNotificationManager.CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = getString(R.string.notification_channel_description)
                    enableVibration(true)
                    enableLights(true)
                }
                nm.createNotificationChannel(channel)
            }
        }

        val displayName = prayerDisplayName(prayerName)

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingTap = PendingIntent.getActivity(
            this, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PrayerAdhanService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, PrayerAdhanService::class.java).apply {
            action = ACTION_DISMISS
        }
        val pendingDismiss = PendingIntent.getService(
            this, 2, dismissIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, PrayerNotificationManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(resources, R.drawable.ic_logo))
            .setContentTitle(getString(R.string.notification_title, displayName))
            .setContentText(getString(R.string.notification_text, displayName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(pendingTap)
            .addAction(0, "Stop", pendingStop)
            .addAction(0, "Dismiss", pendingDismiss)

        val notification = builder.build()
        
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID + prayerName.hashCode(),
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                } else {
                    0
                }
            )
        } catch (e: Exception) {
            Log.e("PrayerAdhanService", "startForeground failed", e)
        }
    }

    private suspend fun playAudio() {
        val prefs = applicationContext.prayerLinkDataStore.data.first()
        val soundModeStr = prefs[PreferencesKeys.SOUND_MODE] ?: NotificationSoundMode.ADHAN.name
        val soundMode = runCatching { NotificationSoundMode.valueOf(soundModeStr) }.getOrDefault(NotificationSoundMode.ADHAN)
        val customUriStr = prefs[PreferencesKeys.CUSTOM_AUDIO_URI]

        val audioProvider = com.prayerlink.app.audio.DefaultAdhanAudioProvider(applicationContext)
        val audioUri: Uri? = audioProvider.getAudioUri(soundMode, customUriStr)

        if (audioUri == null) {
            Log.e("PrayerAdhanService", "audioUri is null, stopping playback")
            scheduleAutoStop()
            return
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { }
                .build()
        } else null
        
        focusRequest = request

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && request != null) {
                audioManager?.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(null, android.media.AudioManager.STREAM_ALARM, android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(applicationContext, audioUri)
                prepare()
                start()
            }
            scheduleAutoStop()
        } catch (e: Exception) {
            Log.e("PrayerAdhanService", "Failed to play audio", e)
            scheduleAutoStop()
        }
    }

    private fun scheduleAutoStop() {
        playbackJob?.cancel()
        playbackJob = serviceScope.launch {
            delay(PLAYBACK_DURATION_MS)
            Log.d("PrayerAdhanService", "Auto-stop timer finished")
            stopPlaybackAndService()
        }
    }

    private fun stopPlaybackAndService(removeNotification: Boolean = false) {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("PrayerAdhanService", "Error releasing media player", e)
        } finally {
            mediaPlayer = null
            
            // Abandon audio focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = focusRequest as? android.media.AudioFocusRequest
                if (req != null) {
                    audioManager?.abandonAudioFocusRequest(req)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
            audioManager = null
            focusRequest = null
        }
        
        if (removeNotification) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } else {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        }
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopPlaybackAndService(removeNotification = false)
        serviceScope.cancel()
    }

    private fun prayerDisplayName(enumName: String): String = when (enumName) {
        com.prayerlink.app.data.model.Prayer.FAJR.name    -> getString(R.string.prayer_fajr)
        com.prayerlink.app.data.model.Prayer.DHUHR.name   -> getString(R.string.prayer_dhuhr)
        com.prayerlink.app.data.model.Prayer.ASR.name     -> getString(R.string.prayer_asr)
        com.prayerlink.app.data.model.Prayer.MAGHRIB.name -> getString(R.string.prayer_maghrib)
        com.prayerlink.app.data.model.Prayer.ISHA.name    -> getString(R.string.prayer_isha)
        else -> enumName
    }
}
