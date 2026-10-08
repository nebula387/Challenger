package com.challenger.app.domain

import com.challenger.app.data.model.Challenge
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Утренняя сводка: одно уведомление с картиной дня. Напоминания по каждому
 * делу остаются, сводка их не заменяет — она отвечает на вопрос «что сегодня»,
 * пока ещё ничего не горит.
 */
object Digest {

    const val DEFAULT_MINUTES = 8 * 60

    /** Ближайший момент сводки: сегодня, если время ещё не прошло, иначе завтра. */
    fun nextAt(now: LocalDateTime, time: LocalTime): LocalDateTime {
        val today = now.toLocalDate().atTime(time)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** Порядок строк: обязательное сверху, дальше по убыванию важности. */
    fun ordered(challenges: List<Challenge>): List<Challenge> =
        challenges.sortedWith(compareBy({ it.priority.ordinal }, { it.sortOrder }))

    fun timeOf(minutes: Int): LocalTime = LocalTime.of(minutes / 60 % 24, minutes % 60)
}
