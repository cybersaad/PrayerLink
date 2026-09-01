package com.prayerlink.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-schedules prayer alarms after the device reboots.
 * Also enqueues the daily [DailyScheduleWorker] as a safety net.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PrayerAlarmScheduler(context).scheduleNextPrayer()
                DailyScheduleWorker.enqueue(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
