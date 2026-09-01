package com.prayerlink.app.ui.dashboard.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prayerlink.app.data.local.PrayerCompletionEntity
import com.prayerlink.app.data.model.PrayerActivityStats
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.prayerlink.app.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerActivityCard(
    stats: PrayerActivityStats,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val currentMonth = YearMonth.now()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChange(selectedMonth.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                }
                
                AnimatedContent(
                    targetState = selectedMonth,
                    transitionSpec = {
                        if (targetState.isAfter(initialState)) {
                            (slideInVertically { height -> height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> height } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "MonthAnimation"
                ) { month ->
                    Text(
                        text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                IconButton(
                    onClick = { onMonthChange(selectedMonth.plusMonths(1)) },
                    enabled = selectedMonth.isBefore(currentMonth)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Statistics Grid
            ActivityStatsGrid(stats = stats)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Monthly Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(0.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Completed Days: ${stats.perfectDays}", style = MaterialTheme.typography.bodySmall)
                            Text("Partially Completed: ${stats.monthPartiallyCompletedDays}", style = MaterialTheme.typography.bodySmall)
                            Text("Missed Days: ${stats.monthMissedDays}", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Completion Rate: ${stats.thisMonthCompletion}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Monthly Calendar Grid
            MonthlyCalendarGrid(
                selectedMonth = selectedMonth,
                datePrayers = stats.datePrayers,
                onDateTap = { selectedDate = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFF047857), label = "Perfect Day") // Deep Emerald
                LegendItem(color = Color(0xFF10B981), label = "Partial") // Medium Emerald
                LegendItem(color = MaterialTheme.colorScheme.surfaceVariant, label = "None")
                LegendItem(color = Color.Transparent, label = "Today", borderColor = Color(0xFFFFD700))
            }
        }
    }
    
    // Bottom Sheet for Date Details
    if (selectedDate != null) {
        val completedPrayers = stats.datePrayers[selectedDate!!] ?: emptyList()
        val count = completedPrayers.size
        ModalBottomSheet(
            onDismissRequest = { selectedDate = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Date
                Text(
                    text = selectedDate!!.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = getHijriDateString(selectedDate!!),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                // Prayer Status
                Text("Prayer Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                val prayers = listOf("FAJR" to "Fajr", "DHUHR" to "Dhuhr", "ASR" to "Asr", "MAGHRIB" to "Maghrib", "ISHA" to "Isha")
                val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
                
                prayers.forEach { (enumName, displayName) ->
                    val completion = completedPrayers.find { it.prayer.name == enumName }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (completion != null) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Completed", tint = Color(0xFF10B981))
                            } else {
                                Icon(Icons.Outlined.Cancel, contentDescription = "Missed", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                        
                        val timeStr = if (completion != null) {
                            if (completion.completedAt != null) {
                                val instant = java.time.Instant.ofEpochMilli(completion.completedAt)
                                val zdt = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                                zdt.format(timeFormatter)
                            } else {
                                "Completed"
                            }
                        } else {
                            "Not Completed"
                        }
                        
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Summary
                Text("Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Completed:", style = MaterialTheme.typography.bodyLarge)
                    Text("$count / 5 Prayers", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                val pct = (count / 5f * 100).toInt()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Completion:", style = MaterialTheme.typography.bodyLarge)
                    Text("$pct%", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Daily Status
                Text("Daily Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                if (count == 5) {
                    Text(
                        text = "Excellent! You completed all prayers today.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "You completed $count of 5 prayers.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, borderColor: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
                .then(
                    if (borderColor != null) Modifier.border(1.5.dp, borderColor, RoundedCornerShape(3.dp))
                    else Modifier
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun getHijriDateString(date: LocalDate): String {
    val hijri = java.time.chrono.HijrahDate.from(date)
    val day = hijri.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
    val month = hijri.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
    val year = hijri.get(java.time.temporal.ChronoField.YEAR)
    
    val resId = when (month) {
        1  -> R.string.hijri_month_1;  2  -> R.string.hijri_month_2
        3  -> R.string.hijri_month_3;  4  -> R.string.hijri_month_4
        5  -> R.string.hijri_month_5;  6  -> R.string.hijri_month_6
        7  -> R.string.hijri_month_7;  8  -> R.string.hijri_month_8
        9  -> R.string.hijri_month_9;  10 -> R.string.hijri_month_10
        11 -> R.string.hijri_month_11; 12 -> R.string.hijri_month_12
        else -> R.string.hijri_month_1
    }
    return "$day ${stringResource(resId)} $year"
}

@Composable
fun ActivityStatsGrid(stats: PrayerActivityStats) {
    val items = listOf(
        Triple("Current Streak", "${stats.currentStreak} Days", Icons.Rounded.LocalFireDepartment),
        Triple("Longest Streak", "${stats.longestStreak} Days", Icons.Rounded.EmojiEvents),
        Triple("Perfect Days", "${stats.perfectDays} Days", Icons.Rounded.EventAvailable),
        Triple("Total Days", "${stats.totalPrayersCompleted}", Icons.Rounded.CalendarMonth),
        Triple("This Month", "${stats.thisMonthCompletion}%", Icons.AutoMirrored.Rounded.TrendingUp),
        Triple("Completion Rate", "${stats.completionRate}%", Icons.Rounded.Insights)
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in items.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = items[i].first,
                    value = items[i].second,
                    icon = items[i].third,
                    modifier = Modifier.weight(1f)
                )
                if (i + 1 < items.size) {
                    StatCard(
                        title = items[i + 1].first,
                        value = items[i + 1].second,
                        icon = items[i + 1].third,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                },
                label = "StatAnimation"
            ) { animatedValue ->
                Text(
                    text = animatedValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MonthlyCalendarGrid(
    selectedMonth: YearMonth,
    datePrayers: Map<LocalDate, List<PrayerCompletionEntity>>,
    onDateTap: (LocalDate) -> Unit
) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val firstDayOfMonth = selectedMonth.atDay(1)
    val daysInMonth = selectedMonth.lengthOfMonth()
    val today = LocalDate.now()
    
    // Day of week is 1 (Mon) to 7 (Sun)
    val leadingEmpty = firstDayOfMonth.dayOfWeek.value - 1
    val totalCells = leadingEmpty + daysInMonth
    val weeks = Math.ceil(totalCells / 7.0).toInt()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Weekday headers
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar cells
        for (week in 0 until weeks) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (dayOfWeek in 0 until 7) {
                    val index = week * 7 + dayOfWeek
                    val dayOfMonth = index - leadingEmpty + 1
                    
                    if (dayOfMonth in 1..daysInMonth) {
                        val date = selectedMonth.atDay(dayOfMonth)
                        DayCard(
                            date = date,
                            isToday = date == today,
                            completedCount = datePrayers[date]?.size ?: 0,
                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                            onClick = { onDateTap(date) }
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DayCard(
    date: LocalDate,
    isToday: Boolean,
    completedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when (completedCount) {
        0 -> MaterialTheme.colorScheme.surfaceVariant
        1, 2 -> Color(0xFF6EE7B7) // Light Emerald
        3, 4 -> Color(0xFF10B981) // Medium Emerald
        else -> Color(0xFF047857) // Deep Emerald
    }
    
    val contentColor = if (completedCount == 0) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
    
    Card(
        modifier = modifier.clickable {
            if (date.isBefore(LocalDate.now().plusDays(1))) {
                onClick()
            }
        },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp),
        border = if (isToday) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700)) else null,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            if (completedCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
