package com.challenger.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.challenger.app.R
import com.challenger.app.data.model.Freeze
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Пауза на несколько дней. Два правила делают её планом, а не отговоркой:
 * окно начинается не раньше завтра, и годовой запас дней ограничен.
 */
@Composable
fun FreezeCard(
    freezes: List<Freeze>,
    daysLeft: Int,
    onPlan: (LocalDate, LocalDate) -> Unit,
    onCancel: (Freeze) -> Unit,
    onEndEarly: (Freeze) -> Unit
) {
    var picking by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val active = freezes.firstOrNull { it.covers(today) }
    val upcoming = freezes.filter { it.startDate.isAfter(today) }

    if (picking) {
        FreezeRangeDialog(
            existing = freezes,
            daysLeft = daysLeft,
            onDismiss = { picking = false },
            onConfirm = { start, end ->
                onPlan(start, end)
                picking = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                stringResource(R.string.settings_freeze),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_freeze_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.settings_freeze_left, daysLeft, Freeze.DAYS_PER_YEAR),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            if (active != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_freeze_active, active.endDate.pretty()),
                    style = MaterialTheme.typography.bodyMedium
                )
                // Болезнь заранее не знает своего срока: если выздоровел раньше,
                // остаток паузы не должен пропадать вместе с напоминаниями.
                if (active.endedEarly(today) != null) {
                    Text(
                        stringResource(R.string.settings_freeze_end_early_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { onEndEarly(active) }) {
                        Text(stringResource(R.string.settings_freeze_end_early))
                    }
                } else {
                    Text(
                        stringResource(R.string.settings_freeze_started),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (upcoming.isEmpty() && active == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_freeze_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            upcoming.forEach { freeze ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            R.string.settings_freeze_planned,
                            freeze.startDate.pretty(),
                            freeze.endDate.pretty()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onCancel(freeze) }) {
                        Text(stringResource(R.string.settings_freeze_remove))
                    }
                }
            }

            if (daysLeft > 0) {
                TextButton(onClick = { picking = true }) {
                    Text(stringResource(R.string.settings_freeze_plan))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FreezeRangeDialog(
    existing: List<Freeze>,
    daysLeft: Int,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    val earliest = Freeze.earliestStart()
    val state = rememberDateRangePickerState(
        selectableDates = object : SelectableDates {
            // Прошлое и уже занятые паузой дни просто не выбираются — так правила
            // видно сразу, а не всплывают отказом после нажатия.
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = utcTimeMillis.toLocalDate()
                return !date.isBefore(earliest) && existing.none { it.covers(date) }
            }
        }
    )

    val start = state.selectedStartDateMillis?.toLocalDate()
    val end = state.selectedEndDateMillis?.toLocalDate() ?: start
    val length = if (start != null && end != null) {
        (ChronoUnit.DAYS.between(start, end) + 1).toInt()
    } else {
        0
    }
    // Оба конца могут быть свободны, а середина — накрывать другую паузу.
    val overlaps = start != null && end != null &&
        existing.any { it.overlaps(Freeze(startDate = start, endDate = end)) }
    val fits = length in 1..daysLeft && !overlaps

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            // Диалог не получает системных отступов, поэтому высоту ограничиваем
            // сами — иначе кнопки уезжают под жестовую полосу.
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                DateRangePicker(state = state, modifier = Modifier.weight(1f))

                val problem = when {
                    overlaps -> stringResource(R.string.settings_freeze_overlap)
                    length > daysLeft -> stringResource(R.string.settings_freeze_too_long, length)
                    else -> null
                }
                if (problem != null) {
                    Text(
                        text = problem,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.editor_time_cancel))
                    }
                    TextButton(
                        onClick = { if (start != null && end != null) onConfirm(start, end) },
                        enabled = fits
                    ) {
                        Text(stringResource(R.string.settings_freeze_confirm))
                    }
                }
            }
        }
    }
}

/** Календарь отдаёт UTC-полночь, поэтому и читаем её в UTC. */
private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun LocalDate.pretty(): String =
    format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
