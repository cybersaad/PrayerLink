package com.prayerlink.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.prayerlink.app.data.calculator.PrayerTimeCalculator
import com.prayerlink.app.data.datasource.PreferencesKeys
import com.prayerlink.app.data.datasource.prayerLinkDataStore
import com.prayerlink.app.data.model.CalculationMethod
import com.prayerlink.app.data.model.Prayer
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules exact alarms for prayer times using [AlarmManager.setAlarmClock].
 *
 * Strategy:
 * 1. Compute today's five prayer times.
 * 2. Find the next future prayer.
 * 3. If all today's prayers have passed, schedule tomorrow's Fajr.
 * 4. Set a single exact alarm; the [PrayerAlarmReceiver] will chain-schedule
 *    the next alarm when it fires.
 */
class PrayerAlarmScheduler(private val context: Context) {

    companion object {
        private const val REQUEST_CODE = 54321
    }

    /**
     * Read user preferences and schedule the next prayer alarm.
     * Safe to call from coroutines (suspend).
     */
    suspend fun scheduleNextPrayer() {
        val prefs = context.prayerLinkDataStore.data.first()

        val enabled = prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
        if (!enabled) {
            cancel()
            return
        }

        val lat = prefs[PreferencesKeys.LATITUDE] ?: 21.4225
        val lon = prefs[PreferencesKeys.LONGITUDE] ?: 39.8262
        val methodIdx = prefs[PreferencesKeys.CALC_METHOD_INDEX] ?: 0
        val asrJ = prefs[PreferencesKeys.ASR_JURISTIC] ?: 0
        val timeZoneId = prefs[PreferencesKeys.TIME_ZONE_ID] ?: ZoneId.systemDefault().id
        val zoneId = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())

        val method = CalculationMethod.entries.getOrElse(methodIdx) {
            CalculationMethod.MUSLIM_WORLD_LEAGUE
        }
        val asrFactor = if (asrJ == 1) 2 else 1

        val now = LocalDateTime.now(zoneId)
        val today = LocalDate.now(zoneId)

        // Today's prayers
        val todayResult = PrayerTimeCalculator.calculate(
            lat, lon, today, zoneId,
            method, asrFactor
        )
        val todayPrayers = listOf(
            Prayer.FAJR to todayResult.fajr,
            Prayer.DHUHR to todayResult.dhuhr,
            Prayer.ASR to todayResult.asr,
            Prayer.MAGHRIB to todayResult.maghrib,
            Prayer.ISHA to todayResult.isha
        )

        // Reschedule missed prayer reminders for the currently active prayer (if any)
        var activePrayer = todayPrayers.lastOrNull { (_, time) -> time.isBefore(now) || time.isEqual(now) }
        var activePrayerDate = today

        if (activePrayer == null) {
            // Check yesterday's Isha
            val yesterday = today.minusDays(1)
            val yesterdayResult = PrayerTimeCalculator.calculate(lat, lon, yesterday, zoneId, method, asrFactor)
            if (yesterdayResult.isha.isBefore(now) || yesterdayResult.isha.isEqual(now)) {
                activePrayer = Prayer.ISHA to yesterdayResult.isha
                activePrayerDate = yesterday
            }
        }

        if (activePrayer != null) {
            PrayerReminderScheduler(context).scheduleRemindersForPrayer(activePrayer.first, activePrayerDate)
        }

        val next = todayPrayers.firstOrNull { (_, time) -> time.isAfter(now) }

        val (prayer, prayerTime) = if (next != null) {
            next
        } else {
            // All today's prayers passed → schedule tomorrow's Fajr
            val tomorrow = today.plusDays(1)
            val tmrResult = PrayerTimeCalculator.calculate(
                lat, lon, tomorrow, zoneId,
                method, asrFactor
            )
            Prayer.FAJR to tmrResult.fajr
        }

        val triggerMs = prayerTime.atZone(zoneId).toInstant().toEpochMilli()
        android.util.Log.d("PrayerAlarmScheduler", "Scheduling alarm for prayer: ${prayer.name} at $prayerTime")

        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.name)
        }
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent, flags
        )

        val am = context.getSystemService(AlarmManager::class.java)
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMs, pi), pi)
            android.util.Log.d("PrayerAlarmScheduler", "setAlarmClock succeeded for ${prayer.name}")
        } catch (e: Exception) {
            android.util.Log.e("PrayerAlarmScheduler", "Failed to schedule alarm", e)
        }
    }

    /** Cancel any pending prayer alarm. */
    fun cancel() {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent, flags
        )
        context.getSystemService(AlarmManager::class.java).cancel(pi)
    }
}
