package com.prayerlink.app.data.model

import java.time.LocalDate

import com.prayerlink.app.data.local.PrayerCompletionEntity

data class PrayerActivityStats(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val perfectDays: Int = 0,
    val totalPrayersCompleted: Int = 0,
    val thisMonthCompletion: Int = 0,
    val completionRate: Int = 0,
    val monthPartiallyCompletedDays: Int = 0,
    val monthMissedDays: Int = 0,
    val datePrayers: Map<LocalDate, List<PrayerCompletionEntity>> = emptyMap()
)
