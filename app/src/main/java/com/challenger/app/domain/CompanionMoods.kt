package com.challenger.app.domain

import java.time.LocalTime

/**
 * Настроение спутницы на главном экране. Оно и есть вся мотивация:
 * утром она спокойна, после первого напоминания ждёт, через несколько часов
 * без отметки грустит, а на выполнении радуется.
 */
enum class CompanionMood {
    /** Утро: первое напоминание ещё не прозвучало. Или сегодня выходной по графику. */
    NEUTRAL,
    /** Напоминание было, дело ещё не закрыто. */
    WAITING,
    /** Прошло несколько часов, а отметки нет. */
    SAD,
    /** Всё обязательное закрыто. */
    HAPPY,
    /** Закрыт весь день. */
    CELEBRATING,
    /** Разовая реакция сразу после отметки, живёт пару секунд. */
    PRAISE;

    val isPositive: Boolean get() = this >= HAPPY
}

object CompanionMoods {

    /**
     * Настроение растёт постепенно: чем больше закрыто, тем она довольнее.
     * Грусть наступает не по часам на стене, а через [sadAfterHours] после
     * первого напоминания — тот же интервал, что у повтора уведомления.
     *
     * @param firstReminder самое раннее напоминание среди сегодняшних дел
     */
    fun moodFor(
        doneCount: Int,
        total: Int,
        mustLeft: Int,
        firstReminder: LocalTime?,
        now: LocalTime,
        sadAfterHours: Int
    ): CompanionMood {
        if (total == 0) return CompanionMood.NEUTRAL
        if (doneCount == total) return CompanionMood.CELEBRATING
        if (mustLeft == 0) return CompanionMood.HAPPY
        // Что-то уже сделано — грустить не за что, ждём остальное.
        if (doneCount > 0) return CompanionMood.WAITING

        if (firstReminder == null) return CompanionMood.WAITING
        if (now < firstReminder) return CompanionMood.NEUTRAL

        val sadFrom = firstReminder.plusHours(sadAfterHours.toLong())
        // plusHours мог перескочить полночь — тогда грустить сегодня уже не о чем.
        val wrapped = sadFrom < firstReminder
        return if (!wrapped && sadAfterHours > 0 && now >= sadFrom) {
            CompanionMood.SAD
        } else {
            CompanionMood.WAITING
        }
    }
}
