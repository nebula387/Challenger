package com.challenger.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.challenger.app.data.model.Category
import com.challenger.app.data.model.Priority
import com.challenger.app.domain.TodayStatus

val Orange40 = Color(0xFFE05A00)
val Orange80 = Color(0xFFFFB68F)
val Teal40 = Color(0xFF00696B)
val Teal80 = Color(0xFF6FF6F9)
val Sand40 = Color(0xFF7A5900)
val Sand80 = Color(0xFFF2C14E)

val DoneGreen = Color(0xFF2E7D32)
val DoneGreenLight = Color(0xFF66BB6A)
val MissRed = Color(0xFFC62828)
val MissRedLight = Color(0xFFEF5350)
val PendingBlue = Color(0xFF1565C0)
val PendingBlueLight = Color(0xFF64B5F6)
val OptionalGrey = Color(0xFF6E6E6E)

/** Цвет приоритета: спорт как must бросается в глаза, учёба — спокойнее. */
fun Priority.color(dark: Boolean): Color = when (this) {
    Priority.MUST -> if (dark) MissRedLight else MissRed
    Priority.NORMAL -> if (dark) PendingBlueLight else PendingBlue
    Priority.OPTIONAL -> OptionalGrey
}

/** Цвет состояния на сегодня: сделано, ждёт, просрочено. */
fun TodayStatus.color(dark: Boolean): Color = when (this) {
    TodayStatus.DONE -> if (dark) DoneGreenLight else DoneGreen
    TodayStatus.PENDING -> if (dark) PendingBlueLight else PendingBlue
    TodayStatus.OVERDUE -> if (dark) MissRedLight else MissRed
    TodayStatus.NOT_TODAY -> OptionalGrey
}

fun Category.accent(dark: Boolean): Color = when (this) {
    Category.SPORT -> if (dark) Orange80 else Orange40
    Category.STUDY -> if (dark) Teal80 else Teal40
    Category.OTHER -> if (dark) Sand80 else Sand40
}
