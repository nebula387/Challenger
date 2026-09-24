package com.challenger.app.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.challenger.app.appContainer
import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Priority
import com.challenger.app.data.prefs.CompanionSettings
import com.challenger.app.domain.Companion
import com.challenger.app.domain.CompanionMood
import com.challenger.app.domain.Schedule
import com.challenger.app.domain.Stats
import com.challenger.app.domain.TodayStatus
import com.challenger.app.domain.todayStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayRow(
    val challenge: Challenge,
    val status: TodayStatus,
    val streak: Int,
    val dayNumber: Int
) {
    val isDone: Boolean get() = status == TodayStatus.DONE
}

data class TodayUiState(
    val date: LocalDate = LocalDate.now(),
    val must: List<TodayRow> = emptyList(),
    val rest: List<TodayRow> = emptyList(),
    val loading: Boolean = true
) {
    val all: List<TodayRow> get() = must + rest
    val doneCount: Int get() = all.count { it.isDone }
    val total: Int get() = all.size
    val mustLeft: Int get() = must.count { !it.isDone }
    val allDone: Boolean get() = total > 0 && doneCount == total

    /** Настроение спутницы — производное от того же прогресса, отдельного состояния нет. */
    val mood: CompanionMood
        get() = Companion.moodFor(
            doneCount = doneCount,
            total = total,
            mustLeft = mustLeft,
            anyOverdue = all.any { it.status == TodayStatus.OVERDUE }
        )
}

class TodayViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = app.appContainer.repository

    val companionSettings: StateFlow<CompanionSettings> = app.appContainer.companionPrefs.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompanionSettings())

    val state: StateFlow<TodayUiState> =
        combine(repo.observeActive(), repo.observeAllCompletions()) { challenges, completions ->
            val today = LocalDate.now()
            val byChallenge = completions.groupBy { it.challengeId }

            val rows = challenges
                .filter { Schedule.isActiveOn(it, today) }
                .map { challenge ->
                    val own = byChallenge[challenge.id].orEmpty()
                    val done = own.any { it.date == today }
                    TodayRow(
                        challenge = challenge,
                        status = todayStatus(challenge, done),
                        streak = Stats.of(challenge, own, today).currentStreak,
                        dayNumber = Schedule.dayNumber(challenge, today)
                    )
                }

            TodayUiState(
                date = today,
                // Обязательное всегда сверху: пропустить его дороже всего.
                must = rows.filter { it.challenge.priority == Priority.MUST }
                    .sortedBy { it.isDone },
                rest = rows.filter { it.challenge.priority != Priority.MUST }
                    .sortedWith(compareBy({ it.isDone }, { it.challenge.priority.ordinal })),
                loading = false
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun toggle(challengeId: Long) {
        viewModelScope.launch { repo.toggleDone(challengeId, LocalDate.now()) }
    }
}
