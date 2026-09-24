package com.challenger.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.companionStore by preferencesDataStore("companion")

/** Как выглядит спутница и показывать ли её вообще. */
data class CompanionSettings(
    val enabled: Boolean = true,
    val packId: String = DEFAULT_PACK,
    val hair: String = "",
    val outfit: String = "",
    val body: String = "",
    val opacity: Float = 1f
) {
    companion object {
        const val DEFAULT_PACK = "default"
    }

    /** Выбранные варианты для подстановки в шаблон имени файла. */
    fun options(): Map<String, String> = buildMap {
        if (hair.isNotBlank()) put("hair", hair)
        if (outfit.isNotBlank()) put("outfit", outfit)
        if (body.isNotBlank()) put("body", body)
    }
}

class CompanionPrefs(private val context: Context) {

    private val keyEnabled = booleanPreferencesKey("enabled")
    private val keyPack = stringPreferencesKey("pack")
    private val keyHair = stringPreferencesKey("hair")
    private val keyOutfit = stringPreferencesKey("outfit")
    private val keyBody = stringPreferencesKey("body")
    private val keyOpacity = floatPreferencesKey("opacity")

    val settings: Flow<CompanionSettings> = context.companionStore.data.map { prefs ->
        CompanionSettings(
            enabled = prefs[keyEnabled] ?: true,
            packId = prefs[keyPack] ?: CompanionSettings.DEFAULT_PACK,
            hair = prefs[keyHair].orEmpty(),
            outfit = prefs[keyOutfit].orEmpty(),
            body = prefs[keyBody].orEmpty(),
            opacity = prefs[keyOpacity] ?: 1f
        )
    }

    suspend fun setEnabled(value: Boolean) =
        context.companionStore.edit { it[keyEnabled] = value }.let { }

    suspend fun setPack(value: String) =
        context.companionStore.edit { it[keyPack] = value }.let { }

    suspend fun setOpacity(value: Float) =
        context.companionStore.edit { it[keyOpacity] = value.coerceIn(0.15f, 1f) }.let { }

    /** Варианты внешности задаются паком, поэтому ключ приходит строкой. */
    suspend fun setOption(name: String, value: String) {
        val key = when (name) {
            "hair" -> keyHair
            "outfit" -> keyOutfit
            "body" -> keyBody
            else -> return
        }
        context.companionStore.edit { it[key] = value }
    }
}
