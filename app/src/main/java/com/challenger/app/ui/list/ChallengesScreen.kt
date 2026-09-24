package com.challenger.app.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.challenger.app.R
import com.challenger.app.data.model.Category
import com.challenger.app.domain.Presets
import com.challenger.app.ui.components.PriorityBadge
import com.challenger.app.ui.components.durationSummary
import com.challenger.app.ui.components.remindersSummary
import com.challenger.app.ui.components.scheduleSummary
import com.challenger.app.ui.theme.LocalIsDark
import com.challenger.app.ui.theme.accent

@Composable
fun ChallengesScreen(
    onOpenChallenge: (Long) -> Unit,
    onCreate: (String?) -> Unit,
    viewModel: ChallengesViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onCreate(null) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.challenges_custom)) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Category.entries.forEach { category ->
                item(key = "header_" + category.name) {
                    CategoryHeader(category)
                }

                val presets = Presets.byCategory(category)
                if (presets.isNotEmpty()) {
                    item(key = "presets_" + category.name) {
                        PresetRow(category, onCreate)
                    }
                }

                items(state.rows(category), key = { it.challenge.id }) { row ->
                    ChallengeListItem(row, onOpenChallenge)
                }

                if (state.rows(category).isEmpty() && presets.isEmpty()) {
                    item(key = "empty_" + category.name) {
                        Text(
                            text = stringResource(R.string.challenges_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun CategoryHeader(category: Category) {
    val dark = LocalIsDark.current
    Text(
        text = category.emoji + "  " + stringResource(category.titleRes),
        style = MaterialTheme.typography.titleLarge,
        color = category.accent(dark),
        modifier = Modifier.padding(top = 14.dp)
    )
}

/** Быстрое добавление: пресет открывает редактор уже заполненным. */
@Composable
private fun PresetRow(category: Category, onCreate: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val presets = Presets.byCategory(category)
        items(presets.size) { index ->
            val preset = presets[index]
            AssistChip(
                onClick = { onCreate(category.name + ":" + index) },
                label = { Text(preset.emoji + " " + stringResource(preset.titleRes)) }
            )
        }
    }
}

@Composable
private fun ChallengeListItem(row: ChallengeRow, onOpenChallenge: (Long) -> Unit) {
    val challenge = row.challenge
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOpenChallenge(challenge.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = challenge.emoji + "  " + challenge.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                PriorityBadge(challenge.priority)
                if (row.streak > 0) {
                    Text(
                        text = "  🔥 " + row.streak,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            val goal = if (challenge.targetValue > 0) {
                challenge.targetValue.toString() + " " + challenge.unit + "  ·  "
            } else {
                ""
            }
            Text(
                text = goal + scheduleSummary(context, challenge) +
                    "  ·  " + durationSummary(context, challenge),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "⏰ " + remindersSummary(context, challenge),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (row.plannedTotal > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { row.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.done_of_total, row.doneCount, row.plannedTotal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (challenge.archived) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.challenges_archived),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
