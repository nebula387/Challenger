package com.challenger.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.challenger.app.appContainer
import com.challenger.app.data.model.Freeze
import com.challenger.app.data.prefs.CompanionSettings
import com.challenger.app.data.prefs.ReminderPrefs
import com.challenger.app.notify.ReminderScheduler
import com.challenger.app.ui.companion.CompanionPack
import com.challenger.app.ui.companion.CompanionPacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.challenger.app.domain.Digest
import java.time.LocalDate
import java.time.LocalTime

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = app.appContainer.repository
    private val prefs = app.appContainer.companionPrefs
    private val reminderPrefs = ReminderPrefs(app)

    val followUpHours: StateFlow<Int> = reminderPrefs.followUpHours
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ReminderPrefs.DEFAULT_FOLLOW_UP_HOURS
        )

    val companion: StateFlow<CompanionSettings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompanionSettings())

    val freezes: StateFlow<List<Freeze>> = repo.observeFreezes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Остаток лимита считается из тех же пауз, поэтому обновляется сам. */
    val freezeDaysLeft: StateFlow<Int> = repo.observeFreezes()
        .map { Freeze.daysLeftIn(LocalDate.now().year, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Freeze.DAYS_PER_YEAR)

    private val _packs = MutableStateFlow<List<CompanionPack>>(emptyList())
    val packs: StateFlow<List<CompanionPack>> = _packs.asStateFlow()

    init {
        viewModelScope.launch {
            _packs.value = withContext(Dispatchers.IO) {
                CompanionPacks.available(getApplication())
            }
        }
    }

    val digestEnabled: StateFlow<Boolean> = reminderPrefs.digestEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val digestMinutes: StateFlow<Int> = reminderPrefs.digestMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Digest.DEFAULT_MINUTES)

    /** Сводка — отдельный будильник, его переставляем сразу после смены настройки. */
    fun setDigestEnabled(value: Boolean) = viewModelScope.launch {
        reminderPrefs.setDigestEnabled(value)
        ReminderScheduler.scheduleDigest(getApplication())
    }

    fun setDigestTime(time: LocalTime) = viewModelScope.launch {
        reminderPrefs.setDigestMinutes(time.hour * 60 + time.minute)
        ReminderScheduler.scheduleDigest(getApplication())
    }

    /** Повтор меняет расписание будильников, поэтому сразу пересобираем их. */
    fun setFollowUpHours(value: Int) = viewModelScope.launch {
        reminderPrefs.setFollowUpHours(value)
        ReminderScheduler.rescheduleAll(getApplication())
    }

    fun planFreeze(start: LocalDate, end: LocalDate) = viewModelScope.launch {
        repo.addFreeze(start, end)
    }

    fun cancelFreeze(freeze: Freeze) = viewModelScope.launch { repo.removeFreeze(freeze) }

    fun endFreezeEarly(freeze: Freeze) = viewModelScope.launch { repo.endFreezeEarly(freeze) }

    fun setEnabled(value: Boolean) = viewModelScope.launch { prefs.setEnabled(value) }
    fun setPack(id: String) = viewModelScope.launch { prefs.setPack(id) }
    fun setOpacity(value: Float) = viewModelScope.launch { prefs.setOpacity(value) }
    fun setFullScreen(value: Boolean) = viewModelScope.launch { prefs.setFullScreen(value) }
    fun setOption(name: String, value: String) =
        viewModelScope.launch { prefs.setOption(name, value) }
}
