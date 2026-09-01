package com.prayerlink.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prayerlink.app.R
import com.prayerlink.app.data.model.Prayer
import com.prayerlink.app.ui.dashboard.components.CountdownCard
import com.prayerlink.app.ui.dashboard.components.DateCard
import com.prayerlink.app.ui.dashboard.components.GreetingHeader
import com.prayerlink.app.ui.dashboard.components.MotivationalCard
import com.prayerlink.app.ui.dashboard.components.PrayerListCard
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Greeting ──────────────────────────────────
            GreetingHeader()

            // ── Dates ─────────────────────────────────────
            DateCard(
                gregorianDate = state.gregorianDate,
                hijriDate = state.hijriDate
            )

            // ── Next Prayer Countdown ──────────────────────
            val next = state.nextPrayer
            if (next != null) {
                CountdownCard(
                    prayerName = next.prayer.localName(),
                    prayerTimeFormatted = next.time.format(timeFormatter),
                    countdown = state.countdown
                )
            } else {
                CountdownCard(
                    prayerName = "All Done",
                    prayerTimeFormatted = "--:--",
                    countdown = state.countdown
                )
            }

            // ── Prayer Times List ──────────────────────────
            PrayerListCard(
                prayers = state.prayers,
                onMarkCompleted = { prayer ->
                    viewModel.markPrayerCompleted(prayer)
                }
            )

            // ── Prayer Activity ────────────────────────────
            com.prayerlink.app.ui.dashboard.components.PrayerActivityCard(
                stats = state.activityStats,
                selectedMonth = state.selectedActivityMonth,
                onMonthChange = viewModel::setActivityMonth
            )

            // ── Motivational Quote ─────────────────────────
            MotivationalCard(quote = state.motivationalQuote)
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Prayer.localName(): String = when (this) {
    Prayer.FAJR    -> stringResource(R.string.prayer_fajr)
    Prayer.DHUHR   -> stringResource(R.string.prayer_dhuhr)
    Prayer.ASR     -> stringResource(R.string.prayer_asr)
    Prayer.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    Prayer.ISHA    -> stringResource(R.string.prayer_isha)
}
