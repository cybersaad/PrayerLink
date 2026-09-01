package com.prayerlink.app.notification

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class ReminderScheduleCalculatorTest {

    @Test
    fun testLongIntervalSchedulesAllReminders() {
        // Interval is 4 hours, all 4 reminders + 1 final warning can fit.
        val startTime = LocalDateTime.of(2023, 1, 1, 12, 0)
        val boundaryTime = LocalDateTime.of(2023, 1, 1, 16, 0)
        val now = LocalDateTime.of(2023, 1, 1, 12, 0)

        val reminders = ReminderScheduleCalculator.calculate(startTime, boundaryTime, now)
        
        assertEquals(5, reminders.size)
        
        assertEquals(PrayerReminderScheduler.TYPE_REMINDER, reminders[0].type)
        assertEquals(startTime.plusMinutes(15), reminders[0].time)
        assertEquals(1, reminders[0].iteration)

        assertEquals(PrayerReminderScheduler.TYPE_REMINDER, reminders[3].type)
        assertEquals(startTime.plusMinutes(60), reminders[3].time)
        assertEquals(4, reminders[3].iteration)

        assertEquals(PrayerReminderScheduler.TYPE_FINAL_WARNING, reminders[4].type)
        assertEquals(boundaryTime.minusMinutes(10), reminders[4].time)
        assertEquals(0, reminders[4].iteration)
    }

    @Test
    fun testShortIntervalTruncatesReminders() {
        // Interval is 40 minutes. Boundary-10 = 30 minutes.
        // Reminders: 15 (fits), 30 (equals boundary-10, strict < means it skips).
        val startTime = LocalDateTime.of(2023, 1, 1, 12, 0)
        val boundaryTime = LocalDateTime.of(2023, 1, 1, 12, 40)
        val now = LocalDateTime.of(2023, 1, 1, 12, 0)

        val reminders = ReminderScheduleCalculator.calculate(startTime, boundaryTime, now)

        assertEquals(2, reminders.size)
        
        assertEquals(PrayerReminderScheduler.TYPE_REMINDER, reminders[0].type)
        assertEquals(startTime.plusMinutes(15), reminders[0].time)

        assertEquals(PrayerReminderScheduler.TYPE_FINAL_WARNING, reminders[1].type)
        assertEquals(startTime.plusMinutes(30), reminders[1].time)
    }

    @Test
    fun testVeryShortIntervalSkipsAllNormalReminders() {
        // Interval is 15 minutes. Boundary-10 = 5 minutes.
        // 15 > 5. No normal reminders.
        val startTime = LocalDateTime.of(2023, 1, 1, 12, 0)
        val boundaryTime = LocalDateTime.of(2023, 1, 1, 12, 15)
        val now = LocalDateTime.of(2023, 1, 1, 12, 0)

        val reminders = ReminderScheduleCalculator.calculate(startTime, boundaryTime, now)

        assertEquals(1, reminders.size)
        assertEquals(PrayerReminderScheduler.TYPE_FINAL_WARNING, reminders[0].type)
        assertEquals(startTime.plusMinutes(5), reminders[0].time)
    }
    
    @Test
    fun testTooShortIntervalSkipsWarningToo() {
        // Interval is 5 minutes. Boundary-10 = -5 minutes.
        // Final warning time < startTime. Everything skipped.
        val startTime = LocalDateTime.of(2023, 1, 1, 12, 0)
        val boundaryTime = LocalDateTime.of(2023, 1, 1, 12, 5)
        val now = LocalDateTime.of(2023, 1, 1, 12, 0)

        val reminders = ReminderScheduleCalculator.calculate(startTime, boundaryTime, now)

        assertEquals(0, reminders.size)
    }

    @Test
    fun testPastAlarmPrevention() {
        // Now is after some reminders have already passed.
        val startTime = LocalDateTime.of(2023, 1, 1, 12, 0)
        val boundaryTime = LocalDateTime.of(2023, 1, 1, 16, 0)
        
        // 35 minutes past start time.
        // 15, 30 are in the past. 45, 60, and warning are in the future.
        val now = LocalDateTime.of(2023, 1, 1, 12, 35)

        val reminders = ReminderScheduleCalculator.calculate(startTime, boundaryTime, now)

        assertEquals(3, reminders.size)
        
        assertEquals(startTime.plusMinutes(45), reminders[0].time)
        assertEquals(3, reminders[0].iteration)

        assertEquals(startTime.plusMinutes(60), reminders[1].time)
        assertEquals(4, reminders[1].iteration)
        
        assertEquals(PrayerReminderScheduler.TYPE_FINAL_WARNING, reminders[2].type)
    }
}
