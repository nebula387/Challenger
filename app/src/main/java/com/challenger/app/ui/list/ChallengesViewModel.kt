package com.challenger.app.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.challenger.app.appContainer
import com.challenger.app.data.model.Category
import com.challenger.app.data.model.Challenge
import com.challenger.app.domain.Schedule
import com.challenger.app.domain.Stats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChallengeRow(
    val challenge: Challenge,
    val streak: Int,
    val doneCount: Int,
    val plannedTotal: Int,
    val progress: Float,
    val activeToday: Boolean,
    val doneToday: Boolean
)

data class ChallengesUiState(
    val byCategory: Map<Category, List<ChallengeRow>> = emptyMap(),
    val loading: Boolean = true
) {
    fun rows(category: Category): List<ChallengeRow> = byCategory[category].orEmpty()
    val isEmpty: Boolean get() = byCategory.values.all { it.isEmpty() }
}

class ChallengesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = app.appContainer.repository

    val state: StateFlow<ChallengesUiState> =
        combine(repo.observeAll(), repo.observeAllCompletions()) { challenges, completions ->
            val today = LocalDate.now()
            val byChallenge = completions.groupBy { it.challengeId }

            val rows = challenges.map { challenge ->
                val own = byChallenge[challenge.id].orEmpty()
                val stats = Stats.of(challenge, own, today)
                ChallengeRow(
                    challenge = challenge,
                    streak = stats.currentStreak,
                    doneCount = stats.doneCount,
                    plannedTotal = stats.plannedTotal,
                    progress = stats.overallProgress,
                    activeToday = Schedule.isActiveOn(challenge, today),
                    doneToday = own.any { it.date == today }
                )
            }

            ChallengesUiState(
                byCategory = Category.entries.associateWith { category ->
                    rows.filter { it.challenge.category == category }
                        .sortedWith(compareBy({ it.challenge.archived }, { it.challenge.sortOrder }))
                },
                loading = false
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChallengesUiState())

    fun toggleToday(challengeId: Long) {
        viewModelScope.launch { repo.toggleDone(challengeId, LocalDate.now()) }
    }

    fun setArchived(challengeId: Long, archived: Boolean) {
        viewModelScope.launch { repo.setArchived(challengeId, archived) }
    }
}
