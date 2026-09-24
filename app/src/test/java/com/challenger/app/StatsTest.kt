package com.challenger.app

import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Completion
import com.challenger.app.data.model.ScheduleType
import com.challenger.app.domain.Stats
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsTest {

    private val start = LocalDate.of(2026, 1, 5)
    private val today = start.plusDays(4)

    private fun challenge(type: ScheduleType = ScheduleType.EVERY_DAY) = Challenge(
        id = 1,
        title = "Тест",
        startDate = start,
        durationDays = 10,
        scheduleType = type
    )

    private fun done(vararg offsets: Long) =
        offsets.map { Completion(1, start.plusDays(it)) }

    @Test
    fun `серия считается подряд назад от сегодня`() {
        val stats = Stats.of(challenge(), done(2, 3, 4), today)
        assertEquals(3, stats.currentStreak)
    }

    @Test
    fun `неотмеченный сегодняшний день серию не рвёт`() {
        // Вчера и позавчера сделаны, сегодня день ещё не кончился.
        val stats = Stats.of(challenge(), done(2, 3), today)
        assertEquals(2, stats.currentStreak)
    }

    @Test
    fun `пропуск в середине обнуляет текущую серию`() {
        val stats = Stats.of(challenge(), done(0, 1, 3), today)
        assertEquals("после пропуска на 2-й день серия — только 3-й", 1, stats.currentStreak)
        assertEquals(2, stats.bestStreak)
    }

    @Test
    fun `считаются выполненные и запланированные дни`() {
        val stats = Stats.of(challenge(), done(0, 1, 2), today)
        assertEquals(3, stats.doneCount)
        assertEquals("с 5 по 9 января — 5 дней", 5, stats.plannedSoFar)
        assertEquals(10, stats.plannedTotal)
        assertEquals(0.6f, stats.rate, 0.001f)
        assertEquals(0.3f, stats.overallProgress, 0.001f)
    }

    @Test
    fun `дни вне графика не портят статистику`() {
        // Через день: активны 5, 7, 9 января. Отмечены все три.
        val stats = Stats.of(challenge(ScheduleType.EVERY_OTHER_DAY), done(0, 2, 4), today)
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.plannedSoFar)
        assertEquals(1.0f, stats.rate, 0.001f)
    }

    @Test
    fun `пустая история даёт нулевую серию`() {
        val stats = Stats.of(challenge(), emptyList(), today)
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.bestStreak)
        assertEquals(0f, stats.rate, 0.001f)
    }
}
