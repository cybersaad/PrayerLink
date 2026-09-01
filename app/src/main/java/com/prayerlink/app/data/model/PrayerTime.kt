package com.prayerlink.app.data.model

import java.time.LocalDateTime

/**
 * Represents the current status of a prayer for the given day.
 */
enum class PrayerState {
    UPCOMING,
    ADHAN_PLAYING,
    WAITING,
    COMPLETED,
    OVERDUE
}

/**
 * Holds a single prayer's computed time and whether it is the next upcoming prayer.
 *
 * @property prayer Which of the five daily prayers this represents.
 * @property time   Calculated local date-time for this prayer today.
 * @property isNext True when this is the very next prayer the user should perform.
 * @property state  The current state of this prayer (e.g., upcoming, completed, overdue).
 */
data class PrayerTime(
    val prayer: Prayer,
    val time: LocalDateTime,
    val isNext: Boolean = false,
    val state: PrayerState = PrayerState.UPCOMING
)
