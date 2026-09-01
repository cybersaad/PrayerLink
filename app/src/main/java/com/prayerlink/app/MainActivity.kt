package com.prayerlink.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.prayerlink.app.data.model.UserSettings
import com.prayerlink.app.data.repository.SettingsRepository
import com.prayerlink.app.notification.DailyScheduleWorker
import com.prayerlink.app.notification.PrayerAlarmScheduler
import com.prayerlink.app.ui.navigation.PrayerLinkNavHost
import com.prayerlink.app.ui.theme.PrayerLinkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single activity hosting the Compose UI.
 *
 * On first launch it:
 * 1. Requests notification permission (Android 13+).
 * 2. Schedules the first prayer alarm.
 * 3. Enqueues the daily WorkManager safety-net.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: alarms work even without the notification permission */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Ensure alarm chain and daily worker are running
        CoroutineScope(Dispatchers.IO).launch {
            PrayerAlarmScheduler(applicationContext).scheduleNextPrayer()
            DailyScheduleWorker.enqueue(applicationContext)
        }

        setContent {
            val settings by settingsRepository.settingsFlow
                .collectAsStateWithLifecycle(initialValue = UserSettings())

            PrayerLinkTheme(themeMode = settings.themeMode) {
                PrayerLinkNavHost()
            }
        }
    }
}
