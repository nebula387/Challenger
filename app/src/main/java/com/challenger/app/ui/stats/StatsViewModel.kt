package com.challenger.app.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.challenger.app.appContainer
import com.challenger.app.data.model.Challenge
import com.challenger.app.domain.ChallengeStats
import com.challenger.app.domain.Schedule
import com.challenger.app.domain.Stats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Один день в календаре-хитмапе. */
enum class DayCell { DONE, MISSED, PENDING, OFF, FUTURE }

data class StatsUiState(
    val challenge: Challenge? = null,
    val stats: ChallengeStats = ChallengeStats(),
    val calendar: List<Pair<LocalDate, DayCell>> = emptyList(),
    val doneToday: Boolean = false,
    val activeToday: Boolean = false
)

class StatsViewModel(
    app: Application,
    handle: SavedStateHandle
) : AndroidViewModel(app) {

    private val repo = app.appContainer.repository
    private val challengeId: Long = handle.get<Long>("id") ?: 0L

    val state: StateFlow<StatsUiState> = combine(
        repo.observeById(challengeId),
        repo.observeCompletions(challengeId)
    ) { challenge, completions ->
        if (challenge == null) return@combine StatsUiState()

        val today = LocalDate.now()
        val done = completions.map { it.date }.toHashSet()

        // Показываем 12 недель назад, выровняв начало на понедельник.
        val from = today.minusWeeks(11).with(java.time.DayOfWeek.MONDAY)
        val to = from.plusWeeks(12).minusDays(1)

        val calendar = generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .map { day ->
                val cell = when {
                    !Schedule.isActiveOn(challenge, day) -> DayCell.OFF
                    day in done -> DayCell.DONE
                    day.isAfter(today) -> DayCell.FUTURE
                    day == today -> DayCell.PENDING
                    else -> DayCell.MISSED
                }
                day to cell
            }
            .toList()

        StatsUiState(
            challenge = challenge,
            stats = Stats.of(challenge, completions, today),
            calendar = calendar,
            doneToday = today in done,
            activeToday = Schedule.isActiveOn(challenge, today)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun toggleToday() {
        viewModelScope.launch { repo.toggleDone(challengeId, LocalDate.now()) }
    }

    fun toggleDay(date: LocalDate, done: Boolean) {
        viewModelScope.launch { repo.setDone(challengeId, date, done) }
    }

    fun setArchived(archived: Boolean) {
        viewModelScope.launch { repo.setArchived(challengeId, archived) }
    }
}
