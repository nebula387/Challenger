package com.challenger.app.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.challenger.app.R
import com.challenger.app.appContainer
import com.challenger.app.data.model.Category
import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Priority
import com.challenger.app.data.model.ScheduleType
import com.challenger.app.data.model.Weekdays
import com.challenger.app.domain.Presets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

data class EditorUiState(
    val draft: Challenge = Challenge(title = ""),
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val saved: Boolean = false
) {
    val canSave: Boolean
        get() = draft.title.isNotBlank() &&
            (draft.scheduleType != ScheduleType.WEEKDAYS || draft.weekdaysMask != 0)
}

class EditorViewModel(
    app: Application,
    handle: SavedStateHandle
) : AndroidViewModel(app) {

    private val repo = app.appContainer.repository
    private val challengeId: Long = handle.get<Long>("id") ?: 0L
    private val presetKey: String = handle.get<String>("preset").orEmpty()

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = if (challengeId > 0) repo.getById(challengeId) else null
            val draft = existing ?: presetDraft() ?: blankDraft()
            _state.value = EditorUiState(
                draft = draft,
                isNew = existing == null,
                loading = false
            )
        }
    }

    /** Пустой черновик для своего челленджа: единица берётся из текущего языка. */
    private fun blankDraft() = Challenge(
        title = "",
        unit = getApplication<Application>().getString(R.string.unit_reps)
    )

    /** Пресет приходит строкой "SPORT:3" из списка челленджей. */
    private fun presetDraft(): Challenge? {
        val parts = presetKey.split(":")
        if (parts.size != 2) return null
        val category = runCatching { Category.valueOf(parts[0]) }.getOrNull() ?: return null
        val index = parts[1].toIntOrNull() ?: return null
        return Presets.byCategory(category).getOrNull(index)
            ?.toChallenge(getApplication())
    }

    private fun edit(block: (Challenge) -> Challenge) {
        _state.update { it.copy(draft = block(it.draft)) }
    }

    fun setTitle(value: String) = edit { it.copy(title = value) }
    fun setEmoji(value: String) = edit { it.copy(emoji = value) }
    fun setCategory(value: Category) = edit { it.copy(category = value) }
    fun setPriority(value: Priority) = edit { it.copy(priority = value) }
    fun setUnit(value: String) = edit { it.copy(unit = value) }

    fun setTarget(value: String) = edit {
        it.copy(targetValue = value.filter { c -> c.isDigit() }.take(5).toIntOrNull() ?: 0)
    }

    fun setDuration(value: String) = edit {
        it.copy(durationDays = value.filter { c -> c.isDigit() }.take(4).toIntOrNull() ?: 0)
    }

    fun setScheduleType(value: ScheduleType) = edit {
        // При переходе на дни недели пустая маска заблокировала бы сохранение.
        val mask = if (value == ScheduleType.WEEKDAYS && it.weekdaysMask == 0) {
            Weekdays.ALL
        } else {
            it.weekdaysMask
        }
        it.copy(scheduleType = value, weekdaysMask = mask)
    }

    fun toggleWeekday(day: DayOfWeek) = edit {
        it.copy(weekdaysMask = Weekdays.toggle(it.weekdaysMask, day))
    }

    fun setRemindersEnabled(value: Boolean) = edit { it.copy(remindersEnabled = value) }

    fun addReminder(time: LocalTime) = edit {
        if (it.reminderTimes.any { t -> t == time }) it
        else it.copy(reminderTimes = (it.reminderTimes + time).sorted())
    }

    fun removeReminder(time: LocalTime) = edit {
        it.copy(reminderTimes = it.reminderTimes.filterNot { t -> t == time })
    }

    fun save() {
        val draft = _state.value.draft
        if (!_state.value.canSave) return
        viewModelScope.launch {
            repo.save(draft.copy(title = draft.title.trim()))
            _state.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val draft = _state.value.draft
        viewModelScope.launch {
            if (draft.id > 0) repo.delete(draft)
            _state.update { it.copy(saved = true) }
        }
    }
}
