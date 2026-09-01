package com.prayerlink.app.data.repository

import com.prayerlink.app.data.calculator.PrayerTimeCalculator
import com.prayerlink.app.data.model.CalculationMethod
import com.prayerlink.app.data.model.Prayer
import com.prayerlink.app.data.model.PrayerTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for computing today's prayer times
 * and identifying the next upcoming prayer.
 */
@Singleton
class PrayerRepository @Inject constructor() {

    /**
     * Returns the five daily prayer times for the given parameters,
     * with [PrayerTime.isNext] set on the next upcoming prayer.
     */
    fun getPrayerTimes(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        zoneId: ZoneId,
        calculationMethodIndex: Int,
        asrJuristic: Int
    ): List<PrayerTime> {
        val method = CalculationMethod.entries.getOrElse(calculationMethodIndex) {
            CalculationMethod.MUSLIM_WORLD_LEAGUE
        }
        val asrFactor = if (asrJuristic == 1) 2 else 1

        val result = PrayerTimeCalculator.calculate(
            latitude = latitude,
            longitude = longitude,
            date = date,
            zoneId = zoneId,
            method = method,
            asrFactor = asrFactor
        )

        val now = LocalDateTime.now(zoneId)
        val prayers = listOf(
            PrayerTime(Prayer.FAJR, result.fajr),
            PrayerTime(Prayer.DHUHR, result.dhuhr),
            PrayerTime(Prayer.ASR, result.asr),
            PrayerTime(Prayer.MAGHRIB, result.maghrib),
            PrayerTime(Prayer.ISHA, result.isha)
        )

        // The next prayer is the first one still in the future.
        val nextIndex = prayers.indexOfFirst { it.time.isAfter(now) }
        return prayers.mapIndexed { index, pt ->
            pt.copy(isNext = index == nextIndex)
        }
    }
}
