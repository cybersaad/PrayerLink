package com.prayerlink.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class annotated for Hilt dependency injection.
 */
@HiltAndroidApp
class PrayerLinkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.prayerlink.app.notification.PrayerNotificationManager(this)
        com.prayerlink.app.notification.UpdateCheckWorker.enqueue(this)
    }
}
