package com.challenger.app

import com.challenger.app.data.model.Challenge
import com.challenger.app.domain.Schedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Повтор напоминания: закрыл уведомление, но галочку не поставил. */
class FollowUpTest {

    private val day = LocalDate.of(2026, 10, 6)

    private fun challenge(vararg times: LocalTime) = Challenge(
        id = 1,
        title = "Тест",
        startDate = day,
        durationDays = 30,
        reminderTimes = times.toList()
    )

    private fun at(hour: Int, minute: Int = 0) = LocalDateTime.of(day, LocalTime.of(hour, minute))

    @Test
    fun `без повтора остаются только заданные времена`() {
        val times = Schedule.remindersOn(challenge(LocalTime.of(8, 0)), day, followUpHours = 0)
        assertEquals(listOf(at(8)), times)
    }

    @Test
    fun `повтор добавляется каждые N часов до вечера`() {
        val times = Schedule.remindersOn(challenge(LocalTime.of(8, 0)), day, followUpHours = 3)
        // 23:00 уже за отсечкой в 22:00, поэтому его нет.
        assertEquals(listOf(at(8), at(11), at(14), at(17), at(20)), times)
    }

    @Test
    fun `повтор не перетекает на следующий день`() {
        val times = Schedule.remindersOn(challenge(LocalTime.of(21, 0)), day, followUpHours = 3)
        assertEquals("после 21:00 повтор был бы в полночь", listOf(at(21)), times)
    }

    @Test
    fun `свои времена и повторы объединяются без дублей`() {
        val times = Schedule.remindersOn(
            challenge(LocalTime.of(8, 0), LocalTime.of(14, 0)),
            day,
            followUpHours = 3
        )
        assertEquals(listOf(at(8), at(11), at(14), at(17), at(20)), times)
    }

    @Test
    fun `после пропущенного напоминания следующее сегодня, а не завтра`() {
        val c = challenge(LocalTime.of(8, 0))
        val now = at(9, 30)

        assertEquals(
            "без повтора ждали бы до завтра",
            LocalDateTime.of(day.plusDays(1), LocalTime.of(8, 0)),
            Schedule.nextReminderAt(c, now, followUpHours = 0)
        )
        assertEquals(
            "с повтором напомним сегодня в 11:00",
            at(11),
            Schedule.nextReminderAt(c, now, followUpHours = 3)
        )
    }

    @Test
    fun `отмеченный день повторами не тревожим`() {
        val c = challenge(LocalTime.of(8, 0))

        val next = Schedule.nextReminderAt(
            c,
            now = at(9, 30),
            doneDates = setOf(day),
            followUpHours = 3
        )

        assertEquals(LocalDateTime.of(day.plusDays(1), LocalTime.of(8, 0)), next)
    }

    @Test
    fun `вечером повторы на сегодня заканчиваются`() {
        val c = challenge(LocalTime.of(8, 0))

        val next = Schedule.nextReminderAt(c, now = at(21, 0), followUpHours = 3)

        assertEquals(LocalDateTime.of(day.plusDays(1), LocalTime.of(8, 0)), next)
    }
}
