package com.challenger.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.challenger.app.R
import com.challenger.app.data.model.Weekdays
import com.challenger.app.ui.components.PriorityBadge
import com.challenger.app.ui.components.durationSummary
import com.challenger.app.ui.components.remindersSummary
import com.challenger.app.ui.components.scheduleSummary
import com.challenger.app.ui.theme.DoneGreen
import com.challenger.app.ui.theme.MissRed
import com.challenger.app.ui.theme.PendingBlue
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val challenge = state.challenge
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(challenge?.title ?: stringResource(R.string.stats_challenge)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (challenge != null) {
                        IconButton(onClick = { onEdit(challenge.id) }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.edit)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (challenge == null) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = challenge.emoji + "  " + challenge.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                PriorityBadge(challenge.priority)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = scheduleSummary(context, challenge) +
                    "  ·  " + durationSummary(context, challenge) +
                    "  ·  ⏰ " + remindersSummary(context, challenge),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    stringResource(R.string.stats_streak),
                    state.stats.currentStreak.toString(),
                    Modifier.weight(1f)
                )
                StatTile(
                    stringResource(R.string.stats_best),
                    state.stats.bestStreak.toString(),
                    Modifier.weight(1f)
                )
                StatTile(
                    stringResource(R.string.stats_rate),
                    (state.stats.rate * 100).roundToInt().toString() + "%",
                    Modifier.weight(1f)
                )
            }

            if (state.stats.plannedTotal > 0) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        R.string.done_of_total,
                        state.stats.doneCount,
                        state.stats.plannedTotal
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { state.stats.overallProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            if (state.activeToday) {
                Button(
                    onClick = { viewModel.toggleToday() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(
                            if (state.doneToday) R.string.stats_unmark
                            else R.string.stats_mark_done
                        )
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.stats_rest_day),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(R.string.stats_last_weeks),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(8.dp))
            Heatmap(state.calendar) { date, cell ->
                when (cell) {
                    DayCell.DONE -> viewModel.toggleDay(date, false)
                    DayCell.MISSED, DayCell.PENDING -> viewModel.toggleDay(date, true)
                    else -> Unit
                }
            }

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = { viewModel.setArchived(!challenge.archived) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (challenge.archived) R.string.stats_unarchive
                        else R.string.stats_archive
                    )
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Сетка 7 строк на 12 колонок: пропуски видно сразу, по клетке можно дозакрыть день. */
@Composable
private fun Heatmap(
    days: List<Pair<LocalDate, DayCell>>,
    onClick: (LocalDate, DayCell) -> Unit
) {
    val weeks = days.chunked(7)

    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            DayOfWeek.entries.forEach { day ->
                Box(modifier = Modifier.size(width = 24.dp, height = 20.dp)) {
                    Text(
                        Weekdays.shortName(day),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { (date, cell) -> DayBox(date, cell, onClick) }
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendItem(DoneGreen, stringResource(R.string.stats_legend_done))
        LegendItem(MissRed, stringResource(R.string.stats_legend_missed))
        LegendItem(PendingBlue, stringResource(R.string.stats_legend_today))
    }
}

@Composable
private fun DayBox(date: LocalDate, cell: DayCell, onClick: (LocalDate, DayCell) -> Unit) {
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val color = when (cell) {
        DayCell.DONE -> DoneGreen
        DayCell.MISSED -> MissRed.copy(alpha = 0.55f)
        DayCell.PENDING -> PendingBlue.copy(alpha = 0.35f)
        DayCell.OFF -> surface
        DayCell.FUTURE -> surface.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .then(
                if (cell == DayCell.PENDING) {
                    Modifier.border(1.dp, PendingBlue, RoundedCornerShape(4.dp))
                } else {
                    Modifier
                }
            )
            .clickable(enabled = cell != DayCell.OFF && cell != DayCell.FUTURE) {
                onClick(date, cell)
            }
    )
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.size(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
