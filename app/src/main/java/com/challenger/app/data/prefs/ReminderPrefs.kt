package com.challenger.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reminderStore by preferencesDataStore("reminders")

/**
 * Поведение напоминаний, общее для всех челленджей.
 * Пока это только повтор: если уведомление закрыли, а отметку не поставили,
 * через несколько часов напомним снова.
 */
class ReminderPrefs(private val context: Context) {

    private val keyFollowUp = intPreferencesKey("follow_up_hours")

    val followUpHours: Flow<Int> =
        context.reminderStore.data.map { it[keyFollowUp] ?: DEFAULT_FOLLOW_UP_HOURS }

    suspend fun currentFollowUpHours(): Int = followUpHours.first()

    suspend fun setFollowUpHours(value: Int) {
        context.reminderStore.edit { it[keyFollowUp] = value.coerceIn(0, 12) }
    }

    companion object {
        const val DEFAULT_FOLLOW_UP_HOURS = 3
        /** 0 — повтор выключен. */
        val OPTIONS = listOf(0, 1, 2, 3, 4, 6)
    }
}
