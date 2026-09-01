package com.prayerlink.app.notification

import android.app.NotificationManager
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
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class PrayerActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var historyRepository: PrayerHistoryRepository

    companion object {
        const val ACTION_MARK_COMPLETED = "com.prayerlink.app.ACTION_MARK_COMPLETED"
        const val ACTION_REMIND_LATER = "com.prayerlink.app.ACTION_REMIND_LATER"
        const val ACTION_DISMISS_ADHAN = "com.prayerlink.app.ACTION_DISMISS_ADHAN"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_ITERATION = "iteration"
        const val NOTIFICATION_REMINDER_BASE_ID = 5000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
        val prayerEnum = runCatching { Prayer.valueOf(prayerName) }.getOrNull() ?: return
        val iteration = intent.getIntExtra(EXTRA_ITERATION, 0)
        
        Log.d("PrayerActionReceiver", "Received action: $action for prayer: $prayerName")
        
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_MARK_COMPLETED -> {
                // Cancel notification
                nm.cancel(NOTIFICATION_REMINDER_BASE_ID + prayerEnum.hashCode())
                
                // Update DB and cancel scheduled reminders
                CoroutineScope(Dispatchers.IO).launch {
                    val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val historyDate = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: LocalDate.now()
                    val prayer = runCatching { com.prayerlink.app.data.model.Prayer.valueOf(prayerName) }.getOrNull() ?: return@launch
                    historyRepository.markPrayerCompleted(historyDate, prayer, com.prayerlink.app.data.model.CompletionSource.NOTIFICATION)
                    
                    val scheduler = PrayerReminderScheduler(context)
                    scheduler.cancelReminders(prayer, historyDate)
                    Log.d("PrayerActionReceiver", "Marked $prayerName completed via notification and cancelled reminders")
                }
            }
            ACTION_REMIND_LATER -> {
                // Just cancel current notification. AlarmManager already has the next reminder scheduled if applicable.
                nm.cancel(NOTIFICATION_REMINDER_BASE_ID + prayerEnum.hashCode())
                Log.d("PrayerActionReceiver", "Remind later selected for $prayerName, notification dismissed")
            }
            ACTION_DISMISS_ADHAN -> {
                val stopIntent = Intent(context, PrayerAdhanService::class.java).apply {
                    this.action = PrayerAdhanService.ACTION_STOP
                }
                context.startService(stopIntent)
                Log.d("PrayerActionReceiver", "Forwarded STOP to PrayerAdhanService via Dismiss")
            }
        }
    }
}
