package com.challenger.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.challenger.app.R
import com.challenger.app.data.model.Weekdays
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        content()
    }
}

/** Кружки Пн..Вс: нагляднее списка галочек и занимает одну строку. */
@Composable
fun WeekdayPicker(mask: Int, onToggle: (DayOfWeek) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DayOfWeek.entries.forEach { day ->
            val selected = Weekdays.contains(mask, day)
            FilterChip(
                selected = selected,
                onClick = { onToggle(day) },
                label = { Text(Weekdays.shortName(day)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmojiPicker(selected: String, options: List<String>, onPick: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { emoji ->
            FilterChip(
                selected = emoji == selected,
                onClick = { onPick(emoji) },
                label = { Text(emoji) },
                shape = CircleShape,
                modifier = Modifier.size(width = 52.dp, height = 40.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderChips(
    times: List<LocalTime>,
    onRemove: (LocalTime) -> Unit,
    onAdd: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        times.sorted().forEach { time ->
            InputChip(
                selected = true,
                onClick = { onRemove(time) },
                label = { Text(String.format("%02d:%02d", time.hour, time.minute)) },
                trailingIcon = { Text("×") }
            )
        }
        TextButton(onClick = onAdd) { Text(stringResource(R.string.editor_add_time)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true
    )
    // Циферблатом удобно ставить «примерно вечером», клавиатурой — точное время.
    var keyboardInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_time_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (keyboardInput) {
                    TimeInput(state = pickerState)
                } else {
                    TimePicker(state = pickerState)
                }
                TextButton(onClick = { keyboardInput = !keyboardInput }) {
                    Text(
                        stringResource(
                            if (keyboardInput) R.string.editor_time_dial
                            else R.string.editor_time_keyboard
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalTime.of(pickerState.hour, pickerState.minute))
            }) { Text(stringResource(R.string.editor_time_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.editor_time_cancel)) }
        }
    )
}
