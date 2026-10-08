package com.challenger.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.challenger.app.domain.Digest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reminderStore by preferencesDataStore("reminders")

/**
 * Поведение напоминаний, общее для всех челленджей: повтор, если уведомление
 * закрыли без отметки, и утренняя сводка дня.
 */
class ReminderPrefs(private val context: Context) {

    private val keyFollowUp = intPreferencesKey("follow_up_hours")
    private val keyDigestEnabled = booleanPreferencesKey("digest_enabled")
    private val keyDigestMinutes = intPreferencesKey("digest_minutes")

    val followUpHours: Flow<Int> =
        context.reminderStore.data.map { it[keyFollowUp] ?: DEFAULT_FOLLOW_UP_HOURS }

    suspend fun currentFollowUpHours(): Int = followUpHours.first()

    suspend fun setFollowUpHours(value: Int) {
        context.reminderStore.edit { it[keyFollowUp] = value.coerceIn(0, 12) }
    }

    val digestEnabled: Flow<Boolean> =
        context.reminderStore.data.map { it[keyDigestEnabled] ?: true }

    /** Время сводки в минутах от полуночи. */
    val digestMinutes: Flow<Int> =
        context.reminderStore.data.map { it[keyDigestMinutes] ?: Digest.DEFAULT_MINUTES }

    suspend fun currentDigestEnabled(): Boolean = digestEnabled.first()

    suspend fun currentDigestMinutes(): Int = digestMinutes.first()

    suspend fun setDigestEnabled(value: Boolean) {
        context.reminderStore.edit { it[keyDigestEnabled] = value }
    }

    suspend fun setDigestMinutes(value: Int) {
        context.reminderStore.edit { it[keyDigestMinutes] = value.coerceIn(0, 24 * 60 - 1) }
    }

    companion object {
        const val DEFAULT_FOLLOW_UP_HOURS = 3
        /** 0 — повтор выключен. */
        val OPTIONS = listOf(0, 1, 2, 3, 4, 6)
    }
}
