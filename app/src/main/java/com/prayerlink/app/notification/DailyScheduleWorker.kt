package com.prayerlink.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * WorkManager periodic worker that re-schedules prayer alarms daily.
 *
 * This is a safety-net: if the alarm chain is ever broken (e.g. after
 * an app update or force-stop), this worker ensures alarms are restored
 * within 24 hours.
 */
class DailyScheduleWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            PrayerAlarmScheduler(applicationContext).scheduleNextPrayer()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "daily_prayer_schedule"

        /** Enqueue (or keep) a unique daily periodic worker. */
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyScheduleWorker>(
                24, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
