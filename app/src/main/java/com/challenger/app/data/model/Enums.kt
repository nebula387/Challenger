package com.challenger.app.data.model

import androidx.annotation.StringRes
import com.challenger.app.R
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Раздел главного меню. */
enum class Category(@StringRes val titleRes: Int, val emoji: String) {
    SPORT(R.string.category_sport, "💪"),
    STUDY(R.string.category_study, "📚"),
    OTHER(R.string.category_other, "⭐")
}

/**
 * Приоритет челленджа. Спорт обычно MUST — пропуск ломает серию и подсвечивается красным,
 * учёбу можно поставить OPTIONAL: пропустил — не так страшно.
 */
enum class Priority(
    @StringRes val titleRes: Int,
    @StringRes val shortTitleRes: Int
) {
    MUST(R.string.priority_must, R.string.priority_must_short),
    NORMAL(R.string.priority_normal, R.string.priority_normal_short),
    OPTIONAL(R.string.priority_optional, R.string.priority_optional_short);

    /** Пропуск MUST-челленджа обнуляет серию, остальные — прощаем. */
    val breaksStreakOnMiss: Boolean get() = this == MUST
}

/** Как часто повторяется челлендж. */
enum class ScheduleType(@StringRes val titleRes: Int) {
    EVERY_DAY(R.string.schedule_every_day),
    EVERY_OTHER_DAY(R.string.schedule_every_other_day),
    WEEKDAYS(R.string.schedule_weekdays)
}

/** Маска дней недели: бит 0 = понедельник … бит 6 = воскресенье. */
object Weekdays {
    const val ALL = 0b1111111
    const val WORKDAYS = 0b0011111
    const val WEEKEND = 0b1100000

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)
    fun contains(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0
    fun toggle(mask: Int, day: DayOfWeek): Int = mask xor bit(day)

    /** Короткие названия дней в языке устройства: Пн/Вт… или Mon/Tue… */
    fun shortName(day: DayOfWeek, locale: Locale = Locale.getDefault()): String =
        day.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase(locale) }

    fun selectedNames(mask: Int, locale: Locale = Locale.getDefault()): String =
        DayOfWeek.entries
            .filter { contains(mask, it) }
            .joinToString(", ") { shortName(it, locale) }
}
