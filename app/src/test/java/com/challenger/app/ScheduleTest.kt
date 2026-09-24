package com.challenger.app

import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.ScheduleType
import com.challenger.app.data.model.Weekdays
import com.challenger.app.domain.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleTest {

    private val start = LocalDate.of(2026, 1, 5) // понедельник

    private fun challenge(
        type: ScheduleType = ScheduleType.EVERY_DAY,
        mask: Int = Weekdays.ALL,
        days: Int = 30
    ) = Challenge(
        id = 1,
        title = "Тест",
        startDate = start,
        durationDays = days,
        scheduleType = type,
        weekdaysMask = mask,
        reminderTimes = listOf(LocalTime.of(9, 0))
    )

    @Test
    fun `каждый день активен весь срок`() {
        val c = challenge(days = 3)
        assertTrue(Schedule.isActiveOn(c, start))
        assertTrue(Schedule.isActiveOn(c, start.plusDays(2)))
        assertFalse("день после окончания срока", Schedule.isActiveOn(c, start.plusDays(3)))
        assertFalse("день до начала", Schedule.isActiveOn(c, start.minusDays(1)))
    }

    @Test
    fun `через день считается от даты старта`() {
        val c = challenge(ScheduleType.EVERY_OTHER_DAY)
        assertTrue(Schedule.isActiveOn(c, start))
        assertFalse(Schedule.isActiveOn(c, start.plusDays(1)))
        assertTrue(Schedule.isActiveOn(c, start.plusDays(2)))
        assertFalse(Schedule.isActiveOn(c, start.plusDays(3)))
    }

    @Test
    fun `дни недели учитывают маску`() {
        val c = challenge(ScheduleType.WEEKDAYS, mask = Weekdays.WORKDAYS)
        assertTrue("понедельник", Schedule.isActiveOn(c, start))
        assertTrue("пятница", Schedule.isActiveOn(c, start.plusDays(4)))
        assertFalse("суббота", Schedule.isActiveOn(c, start.plusDays(5)))
        assertFalse("воскресенье", Schedule.isActiveOn(c, start.plusDays(6)))
    }

    @Test
    fun `бессрочный челлендж не заканчивается`() {
        val c = challenge(days = 0)
        assertNull(c.endDate)
        assertTrue(Schedule.isActiveOn(c, start.plusDays(365)))
    }

    @Test
    fun `plannedTotal считает только дни по графику`() {
        val c = challenge(ScheduleType.WEEKDAYS, mask = Weekdays.WORKDAYS, days = 14)
        assertEquals(10, Schedule.plannedTotal(c))
    }

    @Test
    fun `напоминание переносится на следующее время дня`() {
        val c = challenge().copy(reminderTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))
        val now = LocalDateTime.of(start, LocalTime.of(9, 0))
        assertEquals(LocalDateTime.of(start, LocalTime.of(20, 0)), Schedule.nextReminderAt(c, now))
    }

    @Test
    fun `отмеченный день больше не напоминает`() {
        val c = challenge().copy(reminderTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))
        val now = LocalDateTime.of(start, LocalTime.of(9, 0))

        val next = Schedule.nextReminderAt(c, now, doneDates = setOf(start))

        assertEquals(
            "после отметки ждём только следующий день",
            LocalDateTime.of(start.plusDays(1), LocalTime.of(8, 0)),
            next
        )
    }

    @Test
    fun `выключенные напоминания не планируются`() {
        val c = challenge().copy(remindersEnabled = false)
        assertNull(Schedule.nextReminderAt(c, LocalDateTime.of(start, LocalTime.of(0, 1))))
    }

    @Test
    fun `напоминание пропускает дни вне графика`() {
        val c = challenge(ScheduleType.WEEKDAYS, mask = Weekdays.WEEKEND)
        val now = LocalDateTime.of(start, LocalTime.of(12, 0)) // понедельник

        val next = Schedule.nextReminderAt(c, now)

        assertEquals(
            "ближайшая суббота",
            LocalDateTime.of(start.plusDays(5), LocalTime.of(9, 0)),
            next
        )
    }
}
