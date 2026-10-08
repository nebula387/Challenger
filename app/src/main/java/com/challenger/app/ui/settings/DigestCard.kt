package com.challenger.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.challenger.app.R
import com.challenger.app.domain.Digest
import com.challenger.app.ui.editor.TimePickerDialog
import java.time.LocalTime

/** Утренняя сводка: вкл/выкл и время. */
@Composable
fun DigestCard(
    enabled: Boolean,
    minutes: Int,
    onEnabled: (Boolean) -> Unit,
    onTime: (LocalTime) -> Unit
) {
    var picking by remember { mutableStateOf(false) }
    val time = Digest.timeOf(minutes)

    if (picking) {
        TimePickerDialog(
            initial = time,
            onDismiss = { picking = false },
            onConfirm = {
                onTime(it)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_digest),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = enabled, onCheckedChange = onEnabled)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_digest_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (enabled) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(
                            R.string.settings_digest_time,
                            String.format("%02d:%02d", time.hour, time.minute)
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { picking = true }) {
                        Text(stringResource(R.string.settings_digest_change))
                    }
                }
            }
        }
    }
}
