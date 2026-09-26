package com.challenger.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.challenger.app.R
import com.challenger.app.ui.companion.Companion
import com.challenger.app.ui.components.ChallengeCard
import com.challenger.app.ui.components.scheduleSummary
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    onOpenChallenge: (Long) -> Unit,
    onEditChallenge: (Long) -> Unit,
    onAddChallenge: () -> Unit,
    viewModel: TodayViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val companion by viewModel.companionSettings.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Спутница живёт на фоне: карточки со своим фоном ложатся поверх неё.
        Companion(
            mood = state.mood,
            settings = companion,
            modifier = if (companion.fullScreen) {
                Modifier.matchParentSize()
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .padding(bottom = 12.dp)
            }
        )

        // На весь экран заголовок оказывается поверх фотографии, поэтому
        // притеняем верх — иначе дату и прогресс не прочитать.
        if (companion.enabled && companion.fullScreen) {
            val background = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.38f)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                background.copy(alpha = 0.92f),
                                background.copy(alpha = 0f)
                            )
                        )
                    )
            )
        }

        LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { DayHeader(state) }

        if (!state.loading && state.total == 0) {
            item { EmptyToday(onAddChallenge) }
        }

        if (state.must.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.today_section_must)) }
            items(state.must, key = { it.challenge.id }) { row ->
                TodayCard(row, viewModel, onOpenChallenge)
            }
        }

        if (state.rest.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.today_section_rest)) }
            items(state.rest, key = { it.challenge.id }) { row ->
                TodayCard(row, viewModel, onOpenChallenge)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TodayCard(
    row: TodayRow,
    viewModel: TodayViewModel,
    onOpenChallenge: (Long) -> Unit
) {
    val challenge = row.challenge
    val context = LocalContext.current
    val goal = if (challenge.targetValue > 0) {
        challenge.targetValue.toString() + " " + challenge.unit + "  ·  "
    } else {
        ""
    }
    val progress = if (challenge.durationDays > 0) {
        "  ·  " + stringResource(
            R.string.day_x_of_y, row.dayNumber, challenge.durationDays
        )
    } else {
        ""
    }

    ChallengeCard(
        challenge = challenge,
        status = row.status,
        subtitle = goal + scheduleSummary(context, challenge) + progress,
        streak = row.streak,
        onToggle = { viewModel.toggle(challenge.id) },
        onClick = { onOpenChallenge(challenge.id) }
    )
}

@Composable
private fun DayHeader(state: TodayUiState) {
    val locale = Locale.getDefault()
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
    val title = state.date.format(formatter).replaceFirstChar { it.uppercase(locale) }

    val subtitle = when {
        state.total == 0 -> stringResource(R.string.today_nothing_planned)
        state.allDone -> stringResource(R.string.today_all_done)
        state.mustLeft > 0 -> stringResource(R.string.today_must_left, state.mustLeft)
        else -> stringResource(R.string.today_must_clear)
    }

    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (state.total > 0) {
                Text(
                    text = state.doneCount.toString() + " / " + state.total,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (state.total > 0) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.doneCount.toFloat() / state.total },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun EmptyToday(onAddChallenge: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.today_free_day),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.today_free_day_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onAddChallenge) { Text(stringResource(R.string.today_pick_challenge)) }
    }
}
