package com.challenger.app.domain

import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Completion
import com.challenger.app.data.model.ScheduleType
import com.challenger.app.data.model.Weekdays
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Единственное место, где решается «нужно ли делать челлендж в этот день».
 * Все экраны, напоминания и виджет спрашивают именно здесь, чтобы не разъезжались.
 */
object Schedule {

    /** Запланирован ли челлендж на указанную дату. */
    fun isActiveOn(challenge: Challenge, date: LocalDate): Boolean {
        if (challenge.archived) return false
        if (date.isBefore(challenge.startDate)) return false
        challenge.endDate?.let { if (date.isAfter(it)) return false }

        return when (challenge.scheduleType) {
            ScheduleType.EVERY_DAY -> true
            ScheduleType.EVERY_OTHER_DAY ->
                ChronoUnit.DAYS.between(challenge.startDate, date) % 2 == 0L
            ScheduleType.WEEKDAYS ->
                Weekdays.contains(challenge.weekdaysMask, date.dayOfWeek)
        }
    }

    /** Ближайший день (включая [from]), на который челлендж запланирован. */
    fun nextActiveDay(challenge: Challenge, from: LocalDate): LocalDate? {
        val limit = challenge.endDate ?: from.plusYears(1)
        var day = maxOf(from, challenge.startDate)
        while (!day.isAfter(limit)) {
            if (isActiveOn(challenge, day)) return day
            day = day.plusDays(1)
        }
        return null
    }

    /** Все запланированные дни в диапазоне включительно. */
    fun activeDaysBetween(challenge: Challenge, from: LocalDate, to: LocalDate): List<LocalDate> {
        val days = mutableListOf<LocalDate>()
        var day = from
        while (!day.isAfter(to)) {
            if (isActiveOn(challenge, day)) days += day
            day = day.plusDays(1)
        }
        return days
    }

    /** Номер сегодняшнего дня челленджа для подписи «день 5 из 30». */
    fun dayNumber(challenge: Challenge, date: LocalDate): Int =
        (ChronoUnit.DAYS.between(challenge.startDate, date) + 1)
            .coerceAtLeast(0L)
            .toInt()

    /** Сколько всего тренировок/занятий запланировано за весь срок. */
    fun plannedTotal(challenge: Challenge): Int {
        val end = challenge.endDate ?: return 0
        return activeDaysBetween(challenge, challenge.startDate, end).size
    }

    /** Ближайшее время напоминания после [now], уже с учётом расписания дней. */
    fun nextReminderAt(
        challenge: Challenge,
        now: LocalDateTime,
        doneDates: Set<LocalDate> = emptySet()
    ): LocalDateTime? {
        if (!challenge.remindersEnabled || challenge.reminderTimes.isEmpty()) return null
        val times = challenge.reminderTimes.sorted()

        var day = maxOf(now.toLocalDate(), challenge.startDate)
        val limit = challenge.endDate ?: now.toLocalDate().plusYears(1)

        while (!day.isAfter(limit)) {
            // Выполненный день не тревожим — это главное требование: отметил и тишина.
            if (isActiveOn(challenge, day) && day !in doneDates) {
                val candidate = times
                    .map { LocalDateTime.of(day, it) }
                    .firstOrNull { it.isAfter(now) }
                if (candidate != null) return candidate
            }
            day = day.plusDays(1)
        }
        return null
    }
}

/** Сводка по челленджу для карточек и экрана статистики. */
data class ChallengeStats(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val doneCount: Int = 0,
    val plannedSoFar: Int = 0,
    val plannedTotal: Int = 0,
    val dayNumber: Int = 0
) {
    /** Доля выполненных из уже наступивших запланированных дней, 0f..1f. */
    val rate: Float get() = if (plannedSoFar == 0) 0f else doneCount.toFloat() / plannedSoFar

    /** Прогресс по всему сроку челленджа, 0f..1f. */
    val overallProgress: Float
        get() = if (plannedTotal == 0) 0f else (doneCount.toFloat() / plannedTotal).coerceIn(0f, 1f)
}

object Stats {

    fun of(
        challenge: Challenge,
        completions: List<Completion>,
        today: LocalDate = LocalDate.now()
    ): ChallengeStats {
        val done = completions.map { it.date }.toHashSet()
        val lastDay = minOf(today, challenge.endDate ?: today)
        val planned = Schedule.activeDaysBetween(challenge, challenge.startDate, lastDay)

        return ChallengeStats(
            currentStreak = currentStreak(planned, done, today),
            bestStreak = bestStreak(planned, done),
            doneCount = planned.count { it in done },
            plannedSoFar = planned.size,
            plannedTotal = Schedule.plannedTotal(challenge),
            dayNumber = Schedule.dayNumber(challenge, today)
        )
    }

    /**
     * Серия считается назад от последнего запланированного дня.
     * Сегодняшний день, пока он не отмечен, серию не рвёт — день ещё не кончился.
     */
    private fun currentStreak(planned: List<LocalDate>, done: Set<LocalDate>, today: LocalDate): Int {
        var streak = 0
        for (day in planned.asReversed()) {
            when {
                day in done -> streak++
                day == today -> continue
                else -> break
            }
        }
        return streak
    }

    private fun bestStreak(planned: List<LocalDate>, done: Set<LocalDate>): Int {
        var best = 0
        var run = 0
        for (day in planned) {
            if (day in done) {
                run++
                if (run > best) best = run
            } else {
                run = 0
            }
        }
        return best
    }
}

/** Состояние челленджа на сегодня — то, что видит пользователь на главном экране. */
enum class TodayStatus { DONE, PENDING, OVERDUE, NOT_TODAY }

fun todayStatus(
    challenge: Challenge,
    isDone: Boolean,
    now: LocalDateTime = LocalDateTime.now()
): TodayStatus {
    if (!Schedule.isActiveOn(challenge, now.toLocalDate())) return TodayStatus.NOT_TODAY
    if (isDone) return TodayStatus.DONE
    val lastReminder = challenge.reminderTimes.maxOrNull() ?: LocalTime.of(21, 0)
    return if (now.toLocalTime().isAfter(lastReminder)) TodayStatus.OVERDUE else TodayStatus.PENDING
}
