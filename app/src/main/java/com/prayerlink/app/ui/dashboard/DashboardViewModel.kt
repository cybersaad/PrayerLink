package com.prayerlink.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prayerlink.app.R
import com.prayerlink.app.data.model.PrayerTime
import com.prayerlink.app.data.model.UserSettings
import com.prayerlink.app.data.model.PrayerState
import com.prayerlink.app.data.model.PrayerActivityStats
import com.prayerlink.app.data.repository.PrayerHistoryRepository
import com.prayerlink.app.data.repository.PrayerRepository
import com.prayerlink.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.prayerlink.app.data.local.PrayerCompletionEntity
import com.prayerlink.app.data.model.CompletionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale
import javax.inject.Inject

/**
 * Dashboard UI state.
 */
data class DashboardUiState(
    val prayers: List<PrayerTime> = emptyList(),
    val nextPrayer: PrayerTime? = null,
    val countdown: String = "--:--:--",
    val gregorianDate: String = "",
    val hijriDate: String = "",
    val motivationalQuote: String = "",
    val activityStats: PrayerActivityStats = PrayerActivityStats(),
    val selectedActivityMonth: java.time.YearMonth = java.time.YearMonth.now()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: PrayerHistoryRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val ctx get() = getApplication<Application>()

    private var statsJob: Job? = null
    private val monthStatsCache = mutableMapOf<java.time.YearMonth, PrayerActivityStats>()

    init {
        observeSettings()
        observeStats()
        startCountdown()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                settingsRepository.settingsFlow,
                historyRepository.getAllHistory()
            ) { settings, history ->
                Pair(settings, history)
            }.collect { (settings, history) ->
                refreshPrayerTimes(settings, history)
            }
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            _uiState.map { it.selectedActivityMonth }.distinctUntilChanged().collect { month ->
                statsJob?.cancel()
                statsJob = launch {
                    if (monthStatsCache.containsKey(month)) {
                        _uiState.update { it.copy(activityStats = monthStatsCache[month]!!) }
                    }
                    historyRepository.getHistoryForMonth(month.year, month.monthValue).collect { historyForMonth ->
                        val stats = calculateActivityStats(month, historyForMonth)
                        monthStatsCache[month] = stats
                        _uiState.update { it.copy(activityStats = stats) }
                    }
                }
            }
        }
    }

    private fun refreshPrayerTimes(settings: UserSettings, history: List<PrayerCompletionEntity>? = null) {
        val today = LocalDate.now()
        val zoneId = runCatching { ZoneId.of(settings.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        
        val rawPrayers = prayerRepository.getPrayerTimes(
            latitude = settings.latitude,
            longitude = settings.longitude,
            date = today,
            zoneId = zoneId,
            calculationMethodIndex = settings.calculationMethodIndex,
            asrJuristic = settings.asrJuristic
        )

        val now = LocalDateTime.now(zoneId)

        val prayersWithState = rawPrayers.mapIndexed { index, pt ->
            val isCompleted = history?.any { it.date == today && it.prayer == pt.prayer && it.completed } ?: false
            val nextPt = rawPrayers.getOrNull(index + 1)
            
            val isOverdue = if (nextPt != null) {
                now.isAfter(nextPt.time)
            } else {
                now.toLocalDate().isAfter(today)
            }

            val state = when {
                isCompleted -> PrayerState.COMPLETED
                isOverdue -> PrayerState.OVERDUE
                now.isAfter(pt.time) && !isOverdue -> {
                    val durSinceStart = Duration.between(pt.time, now)
                    if (durSinceStart.seconds < 12) PrayerState.ADHAN_PLAYING else PrayerState.WAITING
                }
                else -> PrayerState.UPCOMING
            }
            pt.copy(state = state)
        }

        _uiState.update { state ->
            state.copy(
                prayers = prayersWithState,
                nextPrayer = prayersWithState.firstOrNull { it.isNext },
                gregorianDate = formatGregorian(today),
                hijriDate = formatHijri(today),
                motivationalQuote = randomQuote()
            )
        }
    }

    fun markPrayerCompleted(prayer: com.prayerlink.app.data.model.Prayer) {
        viewModelScope.launch {
            val today = LocalDate.now()
            historyRepository.markPrayerCompleted(today, prayer, CompletionSource.DASHBOARD)
            
            val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            // Cancel any pending interactive reminders for this prayer today
            val uniqueWorkName = "reminder_${prayer.name}_$todayStr"
            androidx.work.WorkManager.getInstance(ctx).cancelUniqueWork(uniqueWorkName)
            
            // Cancel active interactive reminder notification
            val nm = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val notificationId = 5000 + prayer.hashCode() // 5000 is NOTIFICATION_REMINDER_BASE_ID
            nm.cancel(notificationId)
        }
    }

    /** Tick the countdown every second. */
    private fun startCountdown() {
        viewModelScope.launch {
            while (true) {
                val next = _uiState.value.nextPrayer
                if (next != null) {
                    val settings = settingsRepository.settingsFlow.first()
                    val zoneId = runCatching { ZoneId.of(settings.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
                    val now = LocalDateTime.now(zoneId)
                    val dur = Duration.between(now, next.time)
                    if (dur.isNegative) {
                        // Prayer passed — recalculate
                        val settings = settingsRepository.settingsFlow.first()
                        val history = historyRepository.getAllHistory().first()
                        refreshPrayerTimes(settings, history)
                    } else {
                        _uiState.update { it.copy(countdown = formatDuration(dur)) }
                    }
                } else {
                    _uiState.update {
                        it.copy(countdown = ctx.getString(R.string.all_prayers_passed))
                    }
                }
                delay(1000L)
            }
        }
    }

    private suspend fun calculateActivityStats(month: java.time.YearMonth, history: List<PrayerCompletionEntity>): PrayerActivityStats = withContext(Dispatchers.IO) {
        val datePrayers = mutableMapOf<LocalDate, MutableList<PrayerCompletionEntity>>()
        var totalPrayers = 0
        val today = LocalDate.now()
        
        history.filter { it.completed }.forEach { entity ->
            totalPrayers++
            datePrayers.getOrPut(entity.date) { mutableListOf() }.add(entity)
        }
        
        val completedDaysSet = datePrayers.filter { it.value.size == 5 }.keys
        val sortedDays = completedDaysSet.sorted()
        
        // Monthly Streak calculation
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0
        var previousDay: LocalDate? = null
        
        for (day in sortedDays) {
            if (previousDay == null) {
                tempStreak = 1
            } else {
                if (day == previousDay.plusDays(1)) {
                    tempStreak++
                } else {
                    tempStreak = 1
                }
            }
            if (tempStreak > longestStreak) longestStreak = tempStreak
            previousDay = day
        }
        
        // Current streak (walking back from today or yesterday, only within the current month)
        var dateWalker = today
        if (month.year == today.year && month.monthValue == today.monthValue) {
            if (!completedDaysSet.contains(today)) {
                dateWalker = today.minusDays(1)
            }
            while (completedDaysSet.contains(dateWalker) && dateWalker.monthValue == month.monthValue) {
                currentStreak++
                dateWalker = dateWalker.minusDays(1)
            }
        } else {
            // For past months, current streak doesn't make much sense in the same way, but we can set it to the end of month streak
            dateWalker = month.atEndOfMonth()
            while (completedDaysSet.contains(dateWalker) && dateWalker.monthValue == month.monthValue) {
                currentStreak++
                dateWalker = dateWalker.minusDays(1)
            }
        }
        if (currentStreak > longestStreak) longestStreak = currentStreak
        
        val daysPassedMonth = if (month.year == today.year && month.monthValue == today.monthValue) {
            today.dayOfMonth
        } else {
            month.lengthOfMonth()
        }
        
        val monthFullCount = datePrayers.count { it.value.size == 5 }
        val monthPartialCount = datePrayers.count { it.value.size in 1..4 }
        val completionPct = if (daysPassedMonth > 0) (monthFullCount.toFloat() / daysPassedMonth * 100).toInt() else 0
        val overallCompletionPct = if (daysPassedMonth * 5 > 0) (totalPrayers.toFloat() / (daysPassedMonth * 5) * 100).toInt() else 0
        
        PrayerActivityStats(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            perfectDays = monthFullCount,
            totalPrayersCompleted = totalPrayers,
            thisMonthCompletion = completionPct,
            completionRate = overallCompletionPct,
            monthPartiallyCompletedDays = monthPartialCount,
            monthMissedDays = daysPassedMonth - monthFullCount - monthPartialCount,
            datePrayers = datePrayers
        )
    }
    
    fun setActivityMonth(month: java.time.YearMonth) {
        if (month != _uiState.value.selectedActivityMonth) {
            _uiState.update { it.copy(selectedActivityMonth = month) }
        }
    }

    // ── Formatting helpers ───────────────────────────────────

    private fun formatGregorian(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
        return date.format(formatter)
    }

    private fun formatHijri(date: LocalDate): String {
        val hijri = HijrahDate.from(date)
        val day = hijri.get(ChronoField.DAY_OF_MONTH)
        val month = hijri.get(ChronoField.MONTH_OF_YEAR)
        val year = hijri.get(ChronoField.YEAR)
        return "$day ${hijriMonthName(month)} $year"
    }

    private fun hijriMonthName(month: Int): String {
        val resId = when (month) {
            1  -> R.string.hijri_month_1;  2  -> R.string.hijri_month_2
            3  -> R.string.hijri_month_3;  4  -> R.string.hijri_month_4
            5  -> R.string.hijri_month_5;  6  -> R.string.hijri_month_6
            7  -> R.string.hijri_month_7;  8  -> R.string.hijri_month_8
            9  -> R.string.hijri_month_9;  10 -> R.string.hijri_month_10
            11 -> R.string.hijri_month_11; 12 -> R.string.hijri_month_12
            else -> R.string.hijri_month_1
        }
        return ctx.getString(resId)
    }

    private fun formatDuration(dur: Duration): String {
        val totalSec = dur.seconds
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    private fun randomQuote(): String {
        val quotes = ctx.resources.getStringArray(R.array.motivational_quotes)
        return quotes.random()
    }
}
