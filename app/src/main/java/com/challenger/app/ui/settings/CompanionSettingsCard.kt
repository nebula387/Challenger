package com.challenger.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.challenger.app.R
import com.challenger.app.data.prefs.CompanionSettings
import com.challenger.app.ui.companion.CompanionPack

/**
 * Настройка спутницы. Список образов и вариантов внешности приходит из паков
 * в assets, поэтому экран не надо трогать при добавлении новой графики.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanionSettingsCard(
    settings: CompanionSettings,
    packs: List<CompanionPack>,
    onEnabled: (Boolean) -> Unit,
    onPack: (String) -> Unit,
    onOption: (String, String) -> Unit,
    onOpacity: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_companion),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = settings.enabled, onCheckedChange = onEnabled)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_companion_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!settings.enabled) return@Column

            if (packs.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_companion_no_packs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (packs.size > 1) {
                Spacer(Modifier.height(10.dp))
                Label(stringResource(R.string.settings_companion_style))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    packs.forEach { pack ->
                        FilterChip(
                            selected = pack.id == settings.packId,
                            onClick = { onPack(pack.id) },
                            label = { Text(pack.name) }
                        )
                    }
                }
            }

            val current = packs.firstOrNull { it.id == settings.packId }
            current?.options?.forEach { (name, values) ->
                Spacer(Modifier.height(10.dp))
                Label(name.replaceFirstChar { it.uppercase() })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    values.forEach { value ->
                        val selected = settings.options()[name] == value ||
                            (settings.options()[name] == null && values.first() == value)
                        FilterChip(
                            selected = selected,
                            onClick = { onOption(name, value) },
                            label = { Text(value) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Label(stringResource(R.string.settings_companion_opacity))
            Slider(
                value = settings.opacity,
                onValueChange = onOpacity,
                valueRange = 0.15f..1f
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
}
