package com.challenger.app.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.challenger.app.R

/**
 * Язык приложения отдельно от телефона — английский можно включить, не меняя
 * язык всей системы. Названия языков написаны на самих этих языках: человек,
 * случайно переключившийся на незнакомый, всё равно найдёт свой.
 */
private val LANGUAGES = listOf("en" to "English", "ru" to "Русский")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageCard() {
    // Смена языка пересоздаёт активность, поэтому читать текущий раз за
    // композицию достаточно — отдельное состояние не нужно.
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        .substringBefore(',')
        .substringBefore('-')

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_language_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = current.isEmpty(),
                    onClick = { setLanguage(null) },
                    label = { Text(stringResource(R.string.settings_language_system)) }
                )
                LANGUAGES.forEach { (tag, name) ->
                    FilterChip(
                        selected = current == tag,
                        onClick = { setLanguage(tag) },
                        label = { Text(name) }
                    )
                }
            }
        }
    }
}

/** null — вернуться к языку телефона. */
private fun setLanguage(tag: String?) {
    AppCompatDelegate.setApplicationLocales(
        if (tag == null) LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(tag)
    )
}
