package com.challenger.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.challenger.app.appContainer
import com.challenger.app.data.prefs.CompanionSettings
import com.challenger.app.ui.companion.CompanionPack
import com.challenger.app.ui.companion.CompanionPacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.appContainer.companionPrefs

    val companion: StateFlow<CompanionSettings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompanionSettings())

    private val _packs = MutableStateFlow<List<CompanionPack>>(emptyList())
    val packs: StateFlow<List<CompanionPack>> = _packs.asStateFlow()

    init {
        viewModelScope.launch {
            _packs.value = withContext(Dispatchers.IO) {
                CompanionPacks.available(getApplication())
            }
        }
    }

    fun setEnabled(value: Boolean) = viewModelScope.launch { prefs.setEnabled(value) }
    fun setPack(id: String) = viewModelScope.launch { prefs.setPack(id) }
    fun setOpacity(value: Float) = viewModelScope.launch { prefs.setOpacity(value) }
    fun setFullScreen(value: Boolean) = viewModelScope.launch { prefs.setFullScreen(value) }
    fun setOption(name: String, value: String) =
        viewModelScope.launch { prefs.setOption(name, value) }
}
