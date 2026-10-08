package com.challenger.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.challenger.app.R
import com.challenger.app.data.model.Challenge
import com.challenger.app.domain.TodayStatus
import com.challenger.app.ui.theme.LocalIsDark
import com.challenger.app.ui.theme.color

/** Высота карточки: компактная ровно вдвое ниже обычной. */
private val FULL_HEIGHT = 76.dp
private val COMPACT_HEIGHT = 38.dp

/** Карточки лежат поверх спутницы, поэтому фон приглушённый, а не сплошной. */
private const val CARD_ALPHA = 0.72f

/**
 * Карточка челленджа. Большой круг слева — основная кнопка дня:
 * одно касание отмечает выполнение, и напоминание на сегодня пропадает.
 *
 * В [compact] виде остаётся только название: строка с целью и графиком уходит,
 * высота падает вдвое. Так список дольше не доползает до лица спутницы.
 */
@Composable
fun ChallengeCard(
    challenge: Challenge,
    status: TodayStatus,
    subtitle: String,
    streak: Int,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val dark = LocalIsDark.current
    val accent = status.color(dark)
    val done = status == TodayStatus.DONE

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (done) CARD_ALPHA * 0.7f else CARD_ALPHA
            )
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Цветная полоса слева: красная — обязательное, зелёная — сделано.
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(if (compact) COMPACT_HEIGHT else FULL_HEIGHT)
                    .background(accent)
            )

            Spacer(Modifier.width(if (compact) 8.dp else 12.dp))

            CheckCircle(
                checked = done,
                accent = accent,
                compact = compact,
                onToggle = onToggle
            )

            Spacer(Modifier.width(if (compact) 8.dp else 12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = if (compact) 4.dp else 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = challenge.emoji + "  " + challenge.title,
                    style = if (compact) {
                        MaterialTheme.typography.bodyMedium
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    color = if (done) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (!compact) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (streak > 0) {
                Text(
                    text = "🔥 " + streak,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 14.dp)
                )
            } else {
                Spacer(Modifier.width(14.dp))
            }
        }
    }
}

@Composable
private fun CheckCircle(
    checked: Boolean,
    accent: Color,
    compact: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (compact) 30.dp else 40.dp)
            .clip(CircleShape)
            .background(if (checked) accent else Color.Transparent)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.done_mark),
                tint = Color.White,
                modifier = Modifier.size(if (compact) 18.dp else 24.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(if (compact) 20.dp else 26.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f))
            )
        }
    }
}
