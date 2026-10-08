package com.challenger.app

import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Completion
import com.challenger.app.data.model.Freeze
import com.challenger.app.domain.FrozenDays
import com.challenger.app.domain.Schedule
import com.challenger.app.domain.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Пауза: дни отпуска выпадают из расписания, а серия через них перешагивает. */
class FreezeTest {

    private val start = LocalDate.of(2026, 10, 12)

    private fun challenge() = Challenge(
        id = 1,
        title = "Тест",
        startDate = start,
        durationDays = 30,
        reminderTimes = listOf(LocalTime.of(8, 0))
    )

    /** Пауза на 3-й и 4-й день челленджа. */
    private val frozen = FrozenDays { date ->
        Freeze(startDate = start.plusDays(2), endDate = start.plusDays(3)).covers(date)
    }

    private fun done(vararg offsets: Long) =
        offsets.map { Completion(1, start.plusDays(it)) }

    @Test
    fun `день на паузе не запланирован`() {
        val c = challenge()
        assertTrue(Schedule.isActiveOn(c, start.plusDays(1), frozen))
        assertFalse(Schedule.isActiveOn(c, start.plusDays(2), frozen))
        assertFalse(Schedule.isActiveOn(c, start.plusDays(3), frozen))
        assertTrue(Schedule.isActiveOn(c, start.plusDays(4), frozen))
    }

    @Test
    fun `серия перешагивает паузу, а не рвётся на ней`() {
        val today = start.plusDays(5)
        val completions = done(0, 1, 4, 5)

        assertEquals(
            "без паузы 3-й и 4-й дни — пропуски, и серия обрывается",
            2,
            Stats.of(challenge(), completions, today).currentStreak
        )
        assertEquals(
            "с паузой все запланированные дни закрыты",
            4,
            Stats.of(challenge(), completions, today, frozen).currentStreak
        )
    }

    @Test
    fun `пауза не делает из пропуска выполнение`() {
        // 5-й день не на паузе и не отмечен. Берём «сегодня» позже него, иначе
        // сработает отдельное правило: незакрытый сегодняшний день серию не рвёт.
        val stats = Stats.of(challenge(), done(0, 1, 4), start.plusDays(6), frozen)
        assertEquals("пропуск вне паузы обрывает серию", 0, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
    }

    @Test
    fun `незакрытый сегодняшний день серию не рвёт и на паузе`() {
        val stats = Stats.of(challenge(), done(0, 1, 4), start.plusDays(5), frozen)
        assertEquals(3, stats.currentStreak)
    }

    @Test
    fun `на паузе не напоминаем`() {
        val now = LocalDateTime.of(start.plusDays(1), LocalTime.of(12, 0))

        assertEquals(
            "следующее напоминание перепрыгивает оба дня паузы",
            LocalDateTime.of(start.plusDays(4), LocalTime.of(8, 0)),
            Schedule.nextReminderAt(challenge(), now, frozen = frozen)
        )
    }

    @Test
    fun `дни паузы не идут в план`() {
        assertEquals(30, Schedule.plannedTotal(challenge()))
        assertEquals(28, Schedule.plannedTotal(challenge(), frozen))
    }

    @Test
    fun `внезапная болезнь — паузу можно начать сегодня, но не вчера`() {
        val today = LocalDate.of(2026, 10, 9)
        assertEquals(today, Freeze.earliestStart(today))
    }

    @Test
    fun `досрочное окончание оставляет сегодня на паузе`() {
        val today = start.plusDays(2)
        val sick = Freeze(id = 7, startDate = start, endDate = start.plusDays(6))

        val trimmed = sick.endedEarly(today)!!
        assertEquals("тот же id — запись обновляется, а не дублируется", 7L, trimmed.id)
        assertEquals(today, trimmed.endDate)
        assertTrue("сегодня остаётся на паузе", trimmed.covers(today))
        assertFalse("завтра — снова по расписанию", trimmed.covers(today.plusDays(1)))
        assertEquals("из семи дней остались три, четыре вернулись в лимит", 3, trimmed.days)
    }

    @Test
    fun `досрочно заканчивать нечего, если пауза и так кончается сегодня или не идёт`() {
        val freeze = Freeze(startDate = start, endDate = start.plusDays(2))
        assertEquals(null, freeze.endedEarly(start.plusDays(2)))
        assertEquals("ещё не началась", null, freeze.endedEarly(start.minusDays(1)))
        assertEquals("уже кончилась", null, freeze.endedEarly(start.plusDays(5)))
    }

    @Test
    fun `пересечение пауз ловится, соседние окна — нет`() {
        val a = Freeze(startDate = start, endDate = start.plusDays(3))
        assertTrue(a.overlaps(Freeze(startDate = start.plusDays(3), endDate = start.plusDays(5))))
        assertTrue(a.overlaps(Freeze(startDate = start.minusDays(2), endDate = start.plusDays(9))))
        assertFalse(a.overlaps(Freeze(startDate = start.plusDays(4), endDate = start.plusDays(6))))
    }

    @Test
    fun `длина окна считается по обе границы включительно`() {
        val freeze = Freeze(startDate = start, endDate = start.plusDays(6))
        assertEquals(7, freeze.days)
        assertEquals(1, Freeze(startDate = start, endDate = start).days)
    }
}
