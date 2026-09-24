package com.challenger.app.domain

import android.content.Context
import androidx.annotation.StringRes
import com.challenger.app.R
import com.challenger.app.data.model.Category
import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Priority
import com.challenger.app.data.model.ScheduleType
import com.challenger.app.data.model.Weekdays
import java.time.LocalDate
import java.time.LocalTime

/**
 * Готовые пункты меню из описания проекта. Пользователь жмёт на пресет —
 * открывается редактор с разумными значениями, остаётся только подтвердить.
 * Название и единица берутся из ресурсов, поэтому челлендж создаётся
 * на языке, который сейчас выбран в системе.
 */
data class Preset(
    @StringRes val titleRes: Int,
    val emoji: String,
    val category: Category,
    val targetValue: Int,
    @StringRes val unitRes: Int,
    val priority: Priority = Priority.NORMAL,
    val scheduleType: ScheduleType = ScheduleType.EVERY_DAY,
    val weekdaysMask: Int = Weekdays.ALL,
    val durationDays: Int = 30,
    val reminderTime: LocalTime = LocalTime.of(9, 0)
) {
    fun toChallenge(context: Context, sortOrder: Int = 0) = Challenge(
        title = context.getString(titleRes),
        emoji = emoji,
        category = category,
        priority = priority,
        targetValue = targetValue,
        unit = context.getString(unitRes),
        startDate = LocalDate.now(),
        durationDays = durationDays,
        scheduleType = scheduleType,
        weekdaysMask = weekdaysMask,
        remindersEnabled = true,
        reminderTimes = listOf(reminderTime),
        sortOrder = sortOrder
    )
}

object Presets {

    val sport = listOf(
        Preset(R.string.preset_pushups, "🤸", Category.SPORT, 30, R.string.unit_reps,
            Priority.MUST, reminderTime = LocalTime.of(8, 0)),
        Preset(R.string.preset_pullups, "🧗", Category.SPORT, 10, R.string.unit_reps,
            Priority.MUST, ScheduleType.EVERY_OTHER_DAY, reminderTime = LocalTime.of(8, 0)),
        Preset(R.string.preset_barbell, "🏋", Category.SPORT, 5, R.string.unit_sets,
            Priority.MUST, ScheduleType.WEEKDAYS, weekdaysMask = 0b0010101,
            reminderTime = LocalTime.of(19, 0)),
        Preset(R.string.preset_abs, "🔥", Category.SPORT, 50, R.string.unit_reps,
            Priority.MUST, reminderTime = LocalTime.of(8, 0)),
        Preset(R.string.preset_squats, "🦵", Category.SPORT, 40, R.string.unit_reps,
            Priority.MUST, ScheduleType.EVERY_OTHER_DAY, reminderTime = LocalTime.of(8, 0)),
        Preset(R.string.preset_running, "🏃", Category.SPORT, 3, R.string.unit_km,
            Priority.MUST, ScheduleType.WEEKDAYS, weekdaysMask = 0b0101010,
            reminderTime = LocalTime.of(7, 30)),
        Preset(R.string.preset_walk, "🚶", Category.SPORT, 30, R.string.unit_minutes,
            Priority.NORMAL, reminderTime = LocalTime.of(18, 30))
    )

    val study = listOf(
        Preset(R.string.preset_english, "🇬🇧", Category.STUDY, 20,
            R.string.unit_minutes, Priority.NORMAL, reminderTime = LocalTime.of(20, 0)),
        Preset(R.string.preset_spanish, "🇪🇸", Category.STUDY, 20,
            R.string.unit_minutes, Priority.OPTIONAL, ScheduleType.EVERY_OTHER_DAY,
            reminderTime = LocalTime.of(20, 30)),
        Preset(R.string.preset_it, "💻", Category.STUDY, 45, R.string.unit_minutes,
            Priority.NORMAL, ScheduleType.WEEKDAYS, weekdaysMask = Weekdays.WORKDAYS,
            reminderTime = LocalTime.of(21, 0))
    )

    fun byCategory(category: Category): List<Preset> = when (category) {
        Category.SPORT -> sport
        Category.STUDY -> study
        Category.OTHER -> emptyList()
    }

    val emojis = listOf(
        "🔥", "💪", "🤸", "🧗", "🏋",
        "🦵", "🏃", "🚶", "🚴", "🏊",
        "🧘", "⚽", "📚", "💻", "🇬🇧",
        "🇪🇸", "🧠", "✍", "🎸", "🎨",
        "💧", "🥗", "😴", "🧹", "💰",
        "🙏", "⭐", "🎯", "📖", "🗣"
    )

    /** Единицы измерения для чипов в редакторе. */
    val units = listOf(
        R.string.unit_reps, R.string.unit_sets, R.string.unit_minutes,
        R.string.unit_km, R.string.unit_pages, R.string.unit_pieces
    )
}
