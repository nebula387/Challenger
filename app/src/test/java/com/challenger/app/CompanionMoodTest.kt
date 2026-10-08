package com.challenger.app

import com.challenger.app.domain.Companion
import com.challenger.app.domain.CompanionMood
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** Настроение спутницы: утро спокойное, потом ожидание, потом грусть. */
class CompanionMoodTest {

    private val reminder = LocalTime.of(8, 0)

    private fun mood(
        done: Int,
        total: Int,
        mustLeft: Int,
        now: LocalTime,
        sadAfter: Int = 3
    ) = Companion.moodFor(done, total, mustLeft, reminder, now, sadAfter)

    @Test
    fun `в выходной по графику спутница спокойна`() {
        assertEquals(
            CompanionMood.NEUTRAL,
            Companion.moodFor(0, 0, 0, null, LocalTime.of(15, 0), 3)
        )
    }

    @Test
    fun `до первого напоминания настроение нейтральное`() {
        assertEquals(CompanionMood.NEUTRAL, mood(done = 0, total = 2, mustLeft = 2, now = LocalTime.of(7, 0)))
    }

    @Test
    fun `после напоминания она ждёт`() {
        assertEquals(CompanionMood.WAITING, mood(done = 0, total = 2, mustLeft = 2, now = LocalTime.of(9, 30)))
    }

    @Test
    fun `через несколько часов без отметки грустит`() {
        assertEquals(CompanionMood.SAD, mood(done = 0, total = 2, mustLeft = 2, now = LocalTime.of(11, 30)))
    }

    @Test
    fun `с выключенным повтором грусти не наступает`() {
        assertEquals(
            CompanionMood.WAITING,
            mood(done = 0, total = 2, mustLeft = 2, now = LocalTime.of(20, 0), sadAfter = 0)
        )
    }

    @Test
    fun `частичный прогресс грустить не даёт`() {
        assertEquals(
            "что-то уже сделано, грустить не за что",
            CompanionMood.WAITING,
            mood(done = 1, total = 3, mustLeft = 1, now = LocalTime.of(21, 0))
        )
    }

    @Test
    fun `закрытое обязательное — радость`() {
        assertEquals(CompanionMood.HAPPY, mood(done = 2, total = 3, mustLeft = 0, now = LocalTime.of(21, 0)))
    }

    @Test
    fun `закрытый день — торжество`() {
        assertEquals(CompanionMood.CELEBRATING, mood(done = 3, total = 3, mustLeft = 0, now = LocalTime.of(9, 0)))
    }

    @Test
    fun `позднее напоминание не делает грусть из-за перехода через полночь`() {
        val late = Companion.moodFor(
            doneCount = 0, total = 1, mustLeft = 1,
            firstReminder = LocalTime.of(23, 0), now = LocalTime.of(23, 30), sadAfterHours = 3
        )
        assertEquals(CompanionMood.WAITING, late)
    }
}
