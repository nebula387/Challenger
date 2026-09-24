package com.challenger.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.challenger.app.R
import com.challenger.app.data.model.Category
import com.challenger.app.data.model.Priority
import com.challenger.app.data.model.ScheduleType
import com.challenger.app.domain.Presets
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onDone: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }

    // Подписи чипов строятся в обычных лямбдах, поэтому берём строки через Context.
    val context = LocalContext.current
    val getString: (Int) -> String = { context.getString(it) }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    if (showTimePicker) {
        TimePickerDialog(
            initial = LocalTime.of(9, 0),
            onDismiss = { showTimePicker = false },
            onConfirm = {
                viewModel.addReminder(it)
                showTimePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.editor_new else R.string.editor_edit
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { viewModel.delete() }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val draft = state.draft

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionCard(stringResource(R.string.editor_title)) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = viewModel::setTitle,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.editor_title_hint)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }

            SectionCard(stringResource(R.string.editor_icon)) {
                EmojiPicker(draft.emoji, Presets.emojis, viewModel::setEmoji)
            }

            SectionCard(stringResource(R.string.editor_category)) {
                ChipRow(
                    options = Category.entries,
                    selected = draft.category,
                    label = { it.emoji + " " + getString(it.titleRes) },
                    onSelect = viewModel::setCategory
                )
            }

            SectionCard(stringResource(R.string.editor_priority)) {
                ChipRow(
                    options = Priority.entries,
                    selected = draft.priority,
                    label = { getString(it.titleRes) },
                    onSelect = viewModel::setPriority
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.editor_priority_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(stringResource(R.string.editor_goal)) {
                OutlinedTextField(
                    value = if (draft.targetValue > 0) draft.targetValue.toString() else "",
                    onValueChange = viewModel::setTarget,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.editor_goal_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(6.dp))
                ChipRow(
                    options = Presets.units,
                    selected = Presets.units.firstOrNull { getString(it) == draft.unit }
                        ?: Presets.units.first(),
                    label = { getString(it) },
                    onSelect = { viewModel.setUnit(context.getString(it)) }
                )
            }

            SectionCard(stringResource(R.string.editor_schedule)) {
                ChipRow(
                    options = ScheduleType.entries,
                    selected = draft.scheduleType,
                    label = { getString(it.titleRes) },
                    onSelect = viewModel::setScheduleType
                )
                if (draft.scheduleType == ScheduleType.WEEKDAYS) {
                    Spacer(Modifier.height(8.dp))
                    WeekdayPicker(draft.weekdaysMask, viewModel::toggleWeekday)
                }
            }

            SectionCard(stringResource(R.string.editor_duration)) {
                OutlinedTextField(
                    value = if (draft.durationDays > 0) draft.durationDays.toString() else "",
                    onValueChange = viewModel::setDuration,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.editor_duration_label)) },
                    placeholder = { Text(stringResource(R.string.editor_duration_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(6.dp))
                ChipRow(
                    options = listOf(7, 21, 30, 60, 100, 0),
                    selected = draft.durationDays,
                    label = {
                        if (it == 0) getString(R.string.editor_duration_forever)
                        else context.getString(R.string.editor_duration_days, it)
                    },
                    onSelect = { viewModel.setDuration(it.toString()) }
                )
            }

            SectionCard(stringResource(R.string.editor_reminders)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.editor_reminders_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = draft.remindersEnabled,
                        onCheckedChange = viewModel::setRemindersEnabled
                    )
                }
                if (draft.remindersEnabled) {
                    Spacer(Modifier.height(6.dp))
                    ReminderChips(
                        times = draft.reminderTimes,
                        onRemove = viewModel::removeReminder,
                        onAdd = { showTimePicker = true }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSave
            ) {
                Text(stringResource(if (state.isNew) R.string.editor_create else R.string.editor_save))
            }

            if (!state.isNew) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.delete() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.delete))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) }
            )
        }
    }
}
