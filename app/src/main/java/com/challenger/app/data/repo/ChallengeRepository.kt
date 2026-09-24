package com.challenger.app.data.repo

import android.content.Context
import com.challenger.app.data.db.ChallengerDatabase
import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Completion
import com.challenger.app.domain.ChallengeStats
import com.challenger.app.domain.Schedule
import com.challenger.app.domain.Stats
import com.challenger.app.notify.ReminderScheduler
import com.challenger.app.widget.TodayWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

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
            completions.observeByChallenge(challengeId)
        ) { challenge, done ->
            challenge?.let { Stats.of(it, done) } ?: ChallengeStats()
        }

    /** Челленджи на сегодня вместе с отметкой о выполнении. */
    fun observeToday(date: LocalDate = LocalDate.now()): Flow<List<TodayItem>> =
        combine(challenges.observeActive(), completions.observeByDate(date)) { list, done ->
            val doneById = done.associateBy { it.challengeId }
            list.filter { Schedule.isActiveOn(it, date) }
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
        return challenges.getActive()
            .filter { Schedule.isActiveOn(it, date) }
            .map { TodayItem(it, done[it.id]) }
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
