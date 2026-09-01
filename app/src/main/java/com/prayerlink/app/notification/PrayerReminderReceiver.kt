package com.prayerlink.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.prayerlink.app.data.model.Prayer
import com.prayerlink.app.data.repository.PrayerHistoryRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class PrayerReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var historyRepository: PrayerHistoryRepository

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(PrayerReminderScheduler.EXTRA_PRAYER_NAME) ?: return
        val type = intent.getStringExtra(PrayerReminderScheduler.EXTRA_REMINDER_TYPE) ?: return
        val iteration = intent.getIntExtra(PrayerReminderScheduler.EXTRA_REMINDER_ITERATION, 0)
        
        Log.d("PrayerReminderReceiver", "Received $type for $prayerName (iteration: $iteration)")

        val prayer = runCatching { Prayer.valueOf(prayerName) }.getOrNull() ?: return
        
        // Before showing notification, verify it hasn't been completed manually
        CoroutineScope(Dispatchers.IO).launch {
            val today = LocalDate.now(ZoneId.systemDefault())
            val isCompleted = historyRepository.isPrayerCompleted(today, prayer)
            
            if (isCompleted) {
                Log.d("PrayerReminderReceiver", "Prayer $prayerName already completed, ignoring reminder.")
                return@launch
            }
            
            val notificationManager = PrayerNotificationManager(context)
            if (type == PrayerReminderScheduler.TYPE_REMINDER) {
                notificationManager.showMissedPrayerReminder(prayerName, iteration)
            } else if (type == PrayerReminderScheduler.TYPE_FINAL_WARNING) {
                notificationManager.showFinalQazaWarning(prayerName)
            }
        }
    }
}
