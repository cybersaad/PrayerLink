package com.prayerlink.app.data.calculator

import com.batoulapps.adhan.CalculationMethod as AdhanMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.prayerlink.app.data.model.CalculationMethod
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * Computes Islamic prayer times using the batoulapps adhan library (offline).
 */
object PrayerTimeCalculator {

    data class Result(
        val fajr: LocalDateTime,
        val sunrise: LocalDateTime,
        val dhuhr: LocalDateTime,
        val asr: LocalDateTime,
        val maghrib: LocalDateTime,
        val isha: LocalDateTime,
        val midnight: LocalDateTime,
        
        // Boundaries
        val fajrBoundary: LocalDateTime,
        val dhuhrBoundary: LocalDateTime,
        val asrBoundary: LocalDateTime,
        val maghribBoundary: LocalDateTime,
        val ishaBoundary: LocalDateTime
    )

    fun calculate(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        zoneId: ZoneId,
        method: CalculationMethod,
        asrFactor: Int = 1
    ): Result {
        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        
        val params: CalculationParameters = when (method) {
            CalculationMethod.MUSLIM_WORLD_LEAGUE -> AdhanMethod.MUSLIM_WORLD_LEAGUE.getParameters()
            CalculationMethod.ISNA -> AdhanMethod.NORTH_AMERICA.getParameters()
            CalculationMethod.EGYPTIAN -> AdhanMethod.EGYPTIAN.getParameters()
            CalculationMethod.UMM_AL_QURA -> AdhanMethod.UMM_AL_QURA.getParameters()
            CalculationMethod.KARACHI -> AdhanMethod.KARACHI.getParameters()
            CalculationMethod.TEHRAN -> CalculationParameters(17.7, 14.0)
        }
        
        params.madhab = if (asrFactor == 2) Madhab.HANAFI else Madhab.SHAFI
        
        val prayerTimes = PrayerTimes(coordinates, dateComponents, params)
        
        // Convert to LocalDateTime
        val fajr = prayerTimes.fajr.toLocalDateTime(zoneId)
        val sunrise = prayerTimes.sunrise.toLocalDateTime(zoneId)
        val dhuhr = prayerTimes.dhuhr.toLocalDateTime(zoneId)
        val asr = prayerTimes.asr.toLocalDateTime(zoneId)
        val maghrib = prayerTimes.maghrib.toLocalDateTime(zoneId)
        val isha = prayerTimes.isha.toLocalDateTime(zoneId)
        
        // Next day's fajr for midnight calculation
        val tomorrow = date.plusDays(1)
        val tmrComponents = DateComponents(tomorrow.year, tomorrow.monthValue, tomorrow.dayOfMonth)
        val tmrPrayerTimes = PrayerTimes(coordinates, tmrComponents, params)
        val nextFajr = tmrPrayerTimes.fajr.toLocalDateTime(zoneId)
        
        // Islamic Midnight: halfway between Sunset (Maghrib) and next day's Fajr
        // This is explicitly calculated and transparent as per requirements
        val sunsetEpoch = maghrib.atZone(zoneId).toEpochSecond()
        val nextFajrEpoch = nextFajr.atZone(zoneId).toEpochSecond()
        val midnightEpoch = sunsetEpoch + (nextFajrEpoch - sunsetEpoch) / 2
        val midnight = java.time.Instant.ofEpochSecond(midnightEpoch).atZone(zoneId).toLocalDateTime()

        return Result(
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha,
            midnight = midnight,
            
            fajrBoundary = sunrise,
            dhuhrBoundary = asr,
            asrBoundary = maghrib,
            maghribBoundary = isha,
            ishaBoundary = midnight
        )
    }

    private fun Date.toLocalDateTime(zoneId: ZoneId): LocalDateTime {
        return this.toInstant().atZone(zoneId).toLocalDateTime()
    }
}
