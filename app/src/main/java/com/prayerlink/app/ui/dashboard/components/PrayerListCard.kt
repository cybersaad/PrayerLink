package com.prayerlink.app.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prayerlink.app.R
import com.prayerlink.app.data.model.Prayer
import com.prayerlink.app.data.model.PrayerState
import com.prayerlink.app.data.model.PrayerTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

@Composable
fun PrayerListCard(
    prayers: List<PrayerTime>,
    onMarkCompleted: (Prayer) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            prayers.forEachIndexed { index, pt ->
                PrayerRow(
                    prayerTime = pt,
                    onMarkCompleted = { onMarkCompleted(pt.prayer) }
                )
                if (index < prayers.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerRow(
    prayerTime: PrayerTime,
    onMarkCompleted: () -> Unit
) {
    val state = prayerTime.state
    val isNext = prayerTime.isNext
    
    val bgColor = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val textColor = when (state) {
        PrayerState.OVERDUE -> MaterialTheme.colorScheme.error
        else -> if (isNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    }
    val iconTint = when (state) {
        PrayerState.OVERDUE -> MaterialTheme.colorScheme.error
        PrayerState.COMPLETED -> MaterialTheme.colorScheme.primary
        else -> if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = prayerTime.prayer.icon(),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = iconTint
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = prayerTime.prayer.displayName(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium
                ),
                color = textColor
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = prayerTime.time.format(timeFormatter),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium
                ),
                color = textColor
            )
            Spacer(Modifier.width(16.dp))
            
            // Status Icon / Checkbox
            when (state) {
                PrayerState.COMPLETED -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Completed",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                PrayerState.WAITING, PrayerState.ADHAN_PLAYING, PrayerState.OVERDUE -> {
                    IconButton(onClick = onMarkCompleted, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = "Mark as Prayed",
                            tint = if (state == PrayerState.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                PrayerState.UPCOMING -> {
                    Icon(
                        imageVector = if (isNext) Icons.Outlined.NotificationsActive else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Prayer.icon(): ImageVector = when (this) {
    Prayer.FAJR    -> Icons.Outlined.WbTwilight
    Prayer.DHUHR   -> Icons.Outlined.WbSunny
    Prayer.ASR     -> Icons.Outlined.LightMode
    Prayer.MAGHRIB -> Icons.Outlined.Nightlight
    Prayer.ISHA    -> Icons.Outlined.NightsStay
}

@Composable
private fun Prayer.displayName(): String = when (this) {
    Prayer.FAJR    -> stringResource(R.string.prayer_fajr)
    Prayer.DHUHR   -> stringResource(R.string.prayer_dhuhr)
    Prayer.ASR     -> stringResource(R.string.prayer_asr)
    Prayer.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    Prayer.ISHA    -> stringResource(R.string.prayer_isha)
}
