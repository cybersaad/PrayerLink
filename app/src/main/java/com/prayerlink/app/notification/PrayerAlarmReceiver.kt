package com.prayerlink.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.prayerlink.app.data.datasource.PreferencesKeys
import com.prayerlink.app.data.datasource.prayerLinkDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires when an [AlarmManager] prayer alarm triggers.
 *
 * 1. Shows the prayer notification.
 * 2. Chain-schedules the next prayer alarm.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_PRAYER_NAME = "prayer_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        android.util.Log.d("PrayerAlarmReceiver", "Alarm received at ${System.currentTimeMillis()}")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME)
                if (prayerName == null) {
                    android.util.Log.e("PrayerAlarmReceiver", "prayerName extra is missing! Intent action: ${intent.action}")
                    // Don't silently return. Proceed with a fallback so at least *something* plays or it's visible.
                    // Fallback to "Prayer" as a generic name.
                }
                val safePrayerName = prayerName ?: "Prayer"
                android.util.Log.d("PrayerAlarmReceiver", "Triggered for prayer: $safePrayerName")

                val prefs = context.prayerLinkDataStore.data.first()
                if (prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] == false) {
                    android.util.Log.d("PrayerAlarmReceiver", "Notifications disabled, skipping foreground service")
                    return@launch
                }

                // Start Foreground Service
                val serviceIntent = Intent(context, PrayerAdhanService::class.java).apply {
                    action = PrayerAdhanService.ACTION_START
                    putExtra(PrayerAdhanService.EXTRA_PRAYER_NAME, safePrayerName)
                }
                
                android.util.Log.d("PrayerAlarmReceiver", "Starting PrayerAdhanService")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // Chain: schedule the next prayer alarm
                PrayerAlarmScheduler(context).scheduleNextPrayer()
            } catch (e: Exception) {
                android.util.Log.e("PrayerAlarmReceiver", "Error in onReceive", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
