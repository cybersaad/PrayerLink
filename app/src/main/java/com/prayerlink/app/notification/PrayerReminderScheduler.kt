package com.prayerlink.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.prayerlink.app.data.calculator.PrayerTimeCalculator
import com.prayerlink.app.data.datasource.PreferencesKeys
import com.prayerlink.app.data.datasource.prayerLinkDataStore
import com.prayerlink.app.data.model.CalculationMethod
import com.prayerlink.app.data.model.Prayer
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class PrayerReminderScheduler(private val context: Context) {

    companion object {
        const val TYPE_REMINDER = "REMINDER"
        const val TYPE_FINAL_WARNING = "FINAL_WARNING"
        const val EXTRA_PRAYER_NAME = "EXTRA_PRAYER_NAME"
        const val EXTRA_REMINDER_TYPE = "EXTRA_REMINDER_TYPE"
        const val EXTRA_REMINDER_ITERATION = "EXTRA_REMINDER_ITERATION"
    }

    suspend fun scheduleRemindersForPrayer(prayer: Prayer, targetDate: LocalDate? = null) {
        val prefs = context.prayerLinkDataStore.data.first()
        if (prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] == false) return

        val lat = prefs[PreferencesKeys.LATITUDE] ?: 21.4225
        val lon = prefs[PreferencesKeys.LONGITUDE] ?: 39.8262
        val methodIdx = prefs[PreferencesKeys.CALC_METHOD_INDEX] ?: 0
        val asrJ = prefs[PreferencesKeys.ASR_JURISTIC] ?: 0
        val timeZoneId = prefs[PreferencesKeys.TIME_ZONE_ID] ?: ZoneId.systemDefault().id
        val zoneId = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())

        val method = CalculationMethod.entries.getOrElse(methodIdx) { CalculationMethod.MUSLIM_WORLD_LEAGUE }
        val asrFactor = if (asrJ == 1) 2 else 1

        val today = targetDate ?: LocalDate.now(zoneId)
        val result = PrayerTimeCalculator.calculate(lat, lon, today, zoneId, method, asrFactor)

        val (startTime, boundaryTime) = when (prayer) {
            Prayer.FAJR -> result.fajr to result.fajrBoundary
            Prayer.DHUHR -> result.dhuhr to result.dhuhrBoundary
            Prayer.ASR -> result.asr to result.asrBoundary
            Prayer.MAGHRIB -> result.maghrib to result.maghribBoundary
            Prayer.ISHA -> result.isha to result.ishaBoundary
        }

        val am = context.getSystemService(AlarmManager::class.java)

        // Cancel existing before scheduling new ones
        cancelReminders(prayer, today)

        val now = LocalDateTime.now(zoneId)
        val reminders = ReminderScheduleCalculator.calculate(startTime, boundaryTime, now)
        
        reminders.forEach { reminder ->
            scheduleAlarm(
                prayer = prayer,
                date = today,
                time = reminder.time,
                type = reminder.type,
                iteration = reminder.iteration,
                am = am,
                zoneId = zoneId
            )
        }
    }

    private fun scheduleAlarm(
        prayer: Prayer,
        date: LocalDate,
        time: LocalDateTime,
        type: String,
        iteration: Int,
        am: AlarmManager,
        zoneId: ZoneId
    ) {
        val triggerMs = time.atZone(zoneId).toInstant().toEpochMilli()
        val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER_NAME, prayer.name)
            putExtra(EXTRA_REMINDER_TYPE, type)
            putExtra(EXTRA_REMINDER_ITERATION, iteration)
        }
        
        val requestCode = generateRequestCode(prayer, date, type, iteration)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            Log.d("PrayerReminderScheduler", "Scheduled $type for ${prayer.name} at $time (code: $requestCode)")
        } catch (e: SecurityException) {
            Log.e("PrayerReminderScheduler", "Exact alarm permission missing", e)
        }
    }

    fun cancelReminders(prayer: Prayer, date: LocalDate) {
        val am = context.getSystemService(AlarmManager::class.java)
        // Cancel all possible 4 reminders + 1 warning
        for (i in 1..4) {
            val requestCode = generateRequestCode(prayer, date, TYPE_REMINDER, i)
            val intent = Intent(context, PrayerReminderReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
        }

        val warningCode = generateRequestCode(prayer, date, TYPE_FINAL_WARNING, 0)
        val warningIntent = Intent(context, PrayerReminderReceiver::class.java)
        val piWarning = PendingIntent.getBroadcast(
            context, warningCode, warningIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (piWarning != null) {
            am.cancel(piWarning)
            piWarning.cancel()
        }
        
        Log.d("PrayerReminderScheduler", "Cancelled all reminders for ${prayer.name} on $date")
    }

    private fun generateRequestCode(prayer: Prayer, date: LocalDate, type: String, iteration: Int): Int {
        val dateString = "${date.year}${date.monthValue}${date.dayOfMonth}"
        val typeId = if (type == TYPE_REMINDER) 1 else 2
        // Hash combination to ensure uniqueness
        return "${prayer.name}_${dateString}_${typeId}_$iteration".hashCode()
    }
}
