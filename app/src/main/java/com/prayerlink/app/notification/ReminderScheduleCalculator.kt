package com.prayerlink.app.notification

import java.time.LocalDateTime

object ReminderScheduleCalculator {

    data class ScheduledReminder(
        val time: LocalDateTime,
        val type: String,
        val iteration: Int
    )

    fun calculate(
        startTime: LocalDateTime,
        boundaryTime: LocalDateTime,
        now: LocalDateTime
    ): List<ScheduledReminder> {
        val reminders = mutableListOf<ScheduledReminder>()
        val finalWarningTime = boundaryTime.minusMinutes(10)

        // Schedule normal reminders: 15, 30, 45, 60
        val intervals = listOf(15L, 30L, 45L, 60L)
        intervals.forEachIndexed { index, mins ->
            val reminderTime = startTime.plusMinutes(mins)
            // Strict condition: reminder time must be before (Boundary - 10)
            if (reminderTime.isBefore(finalWarningTime) && reminderTime.isAfter(now)) {
                reminders.add(
                    ScheduledReminder(
                        time = reminderTime,
                        type = PrayerReminderScheduler.TYPE_REMINDER,
                        iteration = index + 1
                    )
                )
            }
        }

        // Schedule final warning
        if (finalWarningTime.isAfter(startTime) && finalWarningTime.isAfter(now)) {
            reminders.add(
                ScheduledReminder(
                    time = finalWarningTime,
                    type = PrayerReminderScheduler.TYPE_FINAL_WARNING,
                    iteration = 0
                )
            )
        }

        return reminders
    }
}
