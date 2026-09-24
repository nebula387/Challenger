package com.challenger.app.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.challenger.app.R
import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Priority
import com.challenger.app.data.model.ScheduleType
import com.challenger.app.data.model.Weekdays
import com.challenger.app.ui.theme.LocalIsDark
import com.challenger.app.ui.theme.color

/**
 * Подписи собираются через Context, а не только в Compose: те же строки
 * нужны виджету и уведомлениям.
 */

/** Человеческая подпись расписания: «через день», «Пн, Ср, Пт», «каждый день». */
fun scheduleSummary(context: Context, challenge: Challenge): String =
    when (challenge.scheduleType) {
        ScheduleType.EVERY_DAY -> context.getString(R.string.schedule_every_day)
        ScheduleType.EVERY_OTHER_DAY -> context.getString(R.string.schedule_every_other_day)
        ScheduleType.WEEKDAYS -> when (challenge.weekdaysMask) {
            Weekdays.ALL -> context.getString(R.string.schedule_all_days)
            Weekdays.WORKDAYS -> context.getString(R.string.schedule_workdays)
            Weekdays.WEEKEND -> context.getString(R.string.schedule_weekend)
            0 -> context.getString(R.string.schedule_no_days)
            else -> Weekdays.selectedNames(challenge.weekdaysMask)
        }.replaceFirstChar { it.uppercase() }
    }

/** Подпись срока: «30 дней» или «бессрочно». */
fun durationSummary(context: Context, challenge: Challenge): String =
    if (challenge.durationDays > 0) {
        context.resources.getQuantityString(
            R.plurals.days, challenge.durationDays, challenge.durationDays
        )
    } else {
        context.getString(R.string.duration_forever)
    }

fun remindersSummary(context: Context, challenge: Challenge): String =
    if (!challenge.remindersEnabled || challenge.reminderTimes.isEmpty()) {
        context.getString(R.string.reminders_none)
    } else {
        challenge.reminderTimes.sorted().joinToString(", ") {
            String.format("%02d:%02d", it.hour, it.minute)
        }
    }

@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    val dark = LocalIsDark.current
    val color = priority.color(dark)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(priority.shortTitleRes),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
