package com.challenger.app.data.repo

import android.content.Context
import com.challenger.app.data.db.ChallengerDatabase
import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Completion
import com.challenger.app.data.model.Freeze
import com.challenger.app.data.model.Priority
import com.challenger.app.data.prefs.ReminderPrefs
import com.challenger.app.domain.ChallengeStats
import com.challenger.app.domain.CompanionMood
import com.challenger.app.domain.CompanionMoods
import com.challenger.app.domain.FrozenDays
import com.challenger.app.domain.Schedule
import com.challenger.app.domain.Stats
import com.challenger.app.notify.ReminderScheduler
import com.challenger.app.widget.TodayWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/**
 * Единая точка входа к данным. Любое изменение здесь же пересобирает будильники
 * и обновляет виджет, поэтому экраны, уведомления и рабочий стол не расходятся.
 */
class ChallengeRepository(
    private val context: Context,
    private val db: ChallengerDatabase = ChallengerDatabase.get(context)
) {
    private val challenges = db.challengeDao()
    private val completions = db.completionDao()
    private val freezes = db.freezeDao()

    fun observeActive(): Flow<List<Challenge>> = challenges.observeActive()
    fun observeAll(): Flow<List<Challenge>> = challenges.observeAll()
    fun observeById(id: Long): Flow<Challenge?> = challenges.observeById(id)
    suspend fun getById(id: Long): Challenge? = challenges.getById(id)

    fun observeCompletions(challengeId: Long): Flow<List<Completion>> =
        completions.observeByChallenge(challengeId)

    /** Все отметки сразу: экраны считают серии по ним, не дёргая базу на каждый челлендж. */
    fun observeAllCompletions(): Flow<List<Completion>> = completions.observeAll()

    fun observeStats(challengeId: Long): Flow<ChallengeStats> =
        combine(
            challenges.observeById(challengeId),
            completions.observeByChallenge(challengeId),
            observeFrozen()
        ) { challenge, done, frozen ->
            challenge?.let { Stats.of(it, done, frozen = frozen) } ?: ChallengeStats()
        }

    fun observeFreezes(): Flow<List<Freeze>> = freezes.observeAll()

    /** Паузы в виде, который понимает расписание. */
    fun observeFrozen(): Flow<FrozenDays> = freezes.observeAll().map { ranges ->
        FrozenDays { date -> ranges.any { it.covers(date) } }
    }

    suspend fun frozenDays(): FrozenDays {
        val ranges = freezes.getAll()
        return FrozenDays { date -> ranges.any { it.covers(date) } }
    }

    /** Активная прямо сейчас пауза, если она есть. */
    suspend fun activeFreeze(today: LocalDate = LocalDate.now()): Freeze? =
        freezes.getAll().firstOrNull { it.covers(today) }

    /**
     * Сколько дней паузы ещё можно потратить в этом году.
     * Прошедшие паузы тоже считаются — иначе лимит ничего не ограничивает.
     */
    suspend fun freezeDaysLeft(year: Int = LocalDate.now().year): Int =
        Freeze.daysLeftIn(year, freezes.getAll())

    /**
     * Ставит паузу. Возвращает false, если окно начинается слишком рано
     * или не укладывается в годовой лимит.
     */
    suspend fun addFreeze(start: LocalDate, end: LocalDate): Boolean {
        if (end.isBefore(start)) return false
        if (start.isBefore(Freeze.earliestStart())) return false

        val freeze = Freeze(startDate = start, endDate = end)
        if (freeze.days > freezeDaysLeft()) return false
        // Пересечение посчитало бы одни и те же дни в лимит дважды.
        if (freezes.getAll().any { it.overlaps(freeze) }) return false

        freezes.insert(freeze)
        syncSideEffects()
        return true
    }

    /** Снять целиком можно только ещё не начавшуюся паузу: прошлое она не переписывает. */
    suspend fun removeFreeze(freeze: Freeze): Boolean {
        if (freeze.hasStarted()) return false
        freezes.delete(freeze.id)
        syncSideEffects()
        return true
    }

    /** Выздоровел раньше — с завтра снова по расписанию, остаток дней вернётся в лимит. */
    suspend fun endFreezeEarly(freeze: Freeze): Boolean {
        val trimmed = freeze.endedEarly() ?: return false
        freezes.insert(trimmed)
        syncSideEffects()
        return true
    }

    /** Челленджи на сегодня вместе с отметкой о выполнении. */
    fun observeToday(date: LocalDate = LocalDate.now()): Flow<List<TodayItem>> =
        combine(
            challenges.observeActive(),
            completions.observeByDate(date),
            observeFrozen()
        ) { list, done, frozen ->
            val doneById = done.associateBy { it.challengeId }
            list.filter { Schedule.isActiveOn(it, date, frozen) }
                .map { TodayItem(it, doneById[it.id]) }
        }

    fun observeTodayProgress(date: LocalDate = LocalDate.now()): Flow<Pair<Int, Int>> =
        observeToday(date).map { items -> items.count { it.isDone } to items.size }

    suspend fun save(challenge: Challenge): Long {
        val id = if (challenge.id == 0L) {
            challenges.insert(challenge)
        } else {
            challenges.update(challenge)
            challenge.id
        }
        syncSideEffects()
        return id
    }

    suspend fun delete(challenge: Challenge) {
        completions.removeAllFor(challenge.id)
        challenges.delete(challenge)
        syncSideEffects()
    }

    suspend fun setArchived(id: Long, archived: Boolean) {
        challenges.setArchived(id, archived)
        syncSideEffects()
    }

    /** Отметить или снять отметку. Возвращает новое состояние. */
    suspend fun toggleDone(
        challengeId: Long,
        date: LocalDate = LocalDate.now(),
        value: Int = 0
    ): Boolean {
        val existing = completions.get(challengeId, date)
        val nowDone = existing == null
        if (nowDone) {
            val target = challenges.getById(challengeId)?.targetValue ?: 0
            completions.upsert(
                Completion(challengeId, date, value = if (value > 0) value else target)
            )
        } else {
            completions.remove(challengeId, date)
        }
        syncSideEffects()
        return nowDone
    }

    suspend fun setDone(challengeId: Long, date: LocalDate, done: Boolean) {
        if (done) {
            val target = challenges.getById(challengeId)?.targetValue ?: 0
            completions.upsert(Completion(challengeId, date, value = target))
        } else {
            completions.remove(challengeId, date)
        }
        syncSideEffects()
    }

    /** Снимок дня для виджета и уведомлений, без подписки на Flow. */
    suspend fun todaySnapshot(date: LocalDate = LocalDate.now()): List<TodayItem> {
        val done = completions.getByDate(date).associateBy { it.challengeId }
        val frozen = frozenDays()
        return challenges.getActive()
            .filter { Schedule.isActiveOn(it, date, frozen) }
            .map { TodayItem(it, done[it.id]) }
    }

    /**
     * Настроение спутницы по снимку дня. Виджет и шторка считают его отсюда,
     * чтобы не разойтись с экраном «Сегодня».
     */
    suspend fun todayMood(now: LocalTime = LocalTime.now()): CompanionMood {
        val items = todaySnapshot()
        return CompanionMoods.moodFor(
            doneCount = items.count { it.isDone },
            total = items.size,
            mustLeft = items.count { it.challenge.priority == Priority.MUST && !it.isDone },
            firstReminder = items.flatMap { it.challenge.reminderTimes }.minOrNull(),
            now = now,
            sadAfterHours = ReminderPrefs(context).currentFollowUpHours()
        )
    }

    /** Даты, за которые челлендж уже отмечен, для планировщика напоминаний. */
    suspend fun doneDates(challengeId: Long): Set<LocalDate> =
        completions.getByChallenge(challengeId).map { it.date }.toSet()

    private suspend fun syncSideEffects() {
        ReminderScheduler.rescheduleAll(context)
        TodayWidget.refresh(context)
    }
}

data class TodayItem(
    val challenge: Challenge,
    val completion: Completion?
) {
    val isDone: Boolean get() = completion != null
}
