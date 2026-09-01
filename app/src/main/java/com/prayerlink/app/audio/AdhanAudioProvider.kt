package com.prayerlink.app.audio

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import com.prayerlink.app.data.model.NotificationSoundMode

interface AdhanAudioProvider {
    fun getAudioUri(mode: NotificationSoundMode, customUriStr: String?): Uri?
}

class DefaultAdhanAudioProvider(private val context: Context) : AdhanAudioProvider {
    override fun getAudioUri(mode: NotificationSoundMode, customUriStr: String?): Uri? {
        return when (mode) {
            NotificationSoundMode.ADHAN -> {
                val rawId = context.resources.getIdentifier("adhan_makkah", "raw", context.packageName)
                if (rawId != 0) {
                    Uri.parse("android.resource://${context.packageName}/$rawId")
                } else {
                    Log.w("AdhanAudioProvider", "Raw resource adhan_makkah not found, falling back to system default")
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
            }
            NotificationSoundMode.SYSTEM -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            NotificationSoundMode.CUSTOM -> customUriStr?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }
}
