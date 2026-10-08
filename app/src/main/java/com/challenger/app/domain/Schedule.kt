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
/**
 * Дни, выпавшие из расписания из-за паузы. Отдельный тип, а не список дат:
 * расписание не должно знать, откуда паузы берутся.
 */
fun interface FrozenDays {
    fun covers(date: LocalDate): Boolean

    companion object {
        val NONE = FrozenDays { false }
    }
}

object Schedule {

    /** Позже этого часа повторы не ставим. */
    val LATEST_FOLLOW_UP: LocalTime = LocalTime.of(22, 0)

    /** Запланирован ли челлендж на указанную дату. */
    fun isActiveOn(
        challenge: Challenge,
        date: LocalDate,
        frozen: FrozenDays = FrozenDays.NONE
    ): Boolean {
        if (challenge.archived) return false
        // Пауза вынимает день из расписания целиком: он не запланирован,
        // а значит не может быть ни пропущен, ни засчитан.
        if (frozen.covers(date)) return false
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
    fun nextActiveDay(
        challenge: Challenge,
        from: LocalDate,
        frozen: FrozenDays = FrozenDays.NONE
    ): LocalDate? {
        val limit = challenge.endDate ?: from.plusYears(1)
        var day = maxOf(from, challenge.startDate)
        while (!day.isAfter(limit)) {
            if (isActiveOn(challenge, day, frozen)) return day
            day = day.plusDays(1)
        }
        return null
    }

    /** Все запланированные дни в диапазоне включительно. */
    fun activeDaysBetween(
        challenge: Challenge,
        from: LocalDate,
        to: LocalDate,
        frozen: FrozenDays = FrozenDays.NONE
    ): List<LocalDate> {
        val days = mutableListOf<LocalDate>()
        var day = from
        while (!day.isAfter(to)) {
            if (isActiveOn(challenge, day, frozen)) days += day
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
    fun plannedTotal(challenge: Challenge, frozen: FrozenDays = FrozenDays.NONE): Int {
        val end = challenge.endDate ?: return 0
        return activeDaysBetween(challenge, challenge.startDate, end, frozen).size
    }

    /**
     * Времена напоминаний за один день: заданные пользователем плюс повторы
     * каждые [followUpHours] часов. Повтор нужен для случая «уведомление закрыл,
     * а сделать забыл» — иначе челлендж молча теряется до завтра.
     */
    fun remindersOn(
        challenge: Challenge,
        day: LocalDate,
        followUpHours: Int
    ): List<LocalDateTime> {
        val base = challenge.reminderTimes.sorted().map { LocalDateTime.of(day, it) }
        if (followUpHours <= 0 || base.isEmpty()) return base

        val step = followUpHours.toLong()
        val repeats = generateSequence(base.first().plusHours(step)) { it.plusHours(step) }
            // Переход на следующий день и поздний вечер отсекаем: ночью будить незачем.
            .takeWhile { it.toLocalDate() == day && !it.toLocalTime().isAfter(LATEST_FOLLOW_UP) }

        return (base + repeats).distinct().sorted()
    }

    /** Ближайшее время напоминания после [now], уже с учётом расписания дней. */
    fun nextReminderAt(
        challenge: Challenge,
        now: LocalDateTime,
        doneDates: Set<LocalDate> = emptySet(),
        followUpHours: Int = 0,
        frozen: FrozenDays = FrozenDays.NONE
    ): LocalDateTime? {
        if (!challenge.remindersEnabled || challenge.reminderTimes.isEmpty()) return null

        var day = maxOf(now.toLocalDate(), challenge.startDate)
        val limit = challenge.endDate ?: now.toLocalDate().plusYears(1)

        while (!day.isAfter(limit)) {
            // Выполненный день не тревожим — это главное требование: отметил и тишина.
            if (isActiveOn(challenge, day, frozen) && day !in doneDates) {
                val candidate = remindersOn(challenge, day, followUpHours)
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
        today: LocalDate = LocalDate.now(),
        frozen: FrozenDays = FrozenDays.NONE
    ): ChallengeStats {
        val done = completions.map { it.date }.toHashSet()
        val lastDay = minOf(today, challenge.endDate ?: today)
        val planned = Schedule.activeDaysBetween(challenge, challenge.startDate, lastDay, frozen)

        return ChallengeStats(
            currentStreak = currentStreak(planned, done, today),
            bestStreak = bestStreak(planned, done),
            doneCount = planned.count { it in done },
            plannedSoFar = planned.size,
            plannedTotal = Schedule.plannedTotal(challenge, frozen),
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
    now: LocalDateTime = LocalDateTime.now(),
    frozen: FrozenDays = FrozenDays.NONE
): TodayStatus {
    if (!Schedule.isActiveOn(challenge, now.toLocalDate(), frozen)) return TodayStatus.NOT_TODAY
    if (isDone) return TodayStatus.DONE
    val lastReminder = challenge.reminderTimes.maxOrNull() ?: LocalTime.of(21, 0)
    return if (now.toLocalTime().isAfter(lastReminder)) TodayStatus.OVERDUE else TodayStatus.PENDING
}
