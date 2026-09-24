package com.challenger.app.ui.companion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.challenger.app.R
import com.challenger.app.domain.CompanionMood

/**
 * Встроенная заглушка, пока в assets нет настоящей графики.
 * Она не пытается изображать человека — просто честно показывает настроение,
 * чтобы механика работала с первого запуска.
 */
@Composable
fun PlaceholderCompanion(mood: CompanionMood, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (mood == CompanionMood.CELEBRATING) {
            FloatingKisses()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MoodFigure(mood)
            Text(
                text = stringResource(captionFor(mood)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun MoodFigure(mood: CompanionMood) {
    val transition = rememberInfiniteTransition(label = "figure")

    val dancing = mood == CompanionMood.CELEBRATING
    val period = when (mood) {
        CompanionMood.CELEBRATING -> 420
        CompanionMood.HAPPY -> 1100
        else -> 2400
    }

    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(period, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    val tilt = if (dancing) (bounce - 0.5f) * 24f else (bounce - 0.5f) * 3f
    val lift = if (dancing) bounce * 14f else bounce * 4f

    Text(
        text = emojiFor(mood),
        fontSize = 96.sp,
        modifier = Modifier
            .graphicsLayer { translationY = -lift }
            .rotate(tilt)
            .scale(if (dancing) 1f + bounce * 0.06f else 1f)
    )
}

/** Поцелуи и сердечки взлетают вверх, когда весь день закрыт. */
@Composable
private fun FloatingKisses() {
    val symbols = listOf("😘", "💖", "✨", "💋")
    val transition = rememberInfiniteTransition(label = "kisses")

    Box(modifier = Modifier.fillMaxSize()) {
        symbols.forEachIndexed { index, symbol ->
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2600 + index * 350, easing = LinearEasing)
                ),
                label = "kiss$index"
            )

            val drift = (index - symbols.size / 2f) * 46f

            Text(
                text = symbol,
                fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationY = 120f - phase * 320f
                        translationX = drift + kotlin.math.sin(phase * 6f) * 16f
                    }
                    .alpha(((1f - phase) * 1.4f).coerceIn(0f, 1f))
            )
        }
    }
}

private fun emojiFor(mood: CompanionMood): String = when (mood) {
    CompanionMood.SAD -> "🙍‍♀️"
    CompanionMood.BORED -> "🧍‍♀️"
    CompanionMood.INTERESTED -> "💁‍♀️"
    CompanionMood.HAPPY -> "🙆‍♀️"
    CompanionMood.CELEBRATING -> "💃"
}

private fun captionFor(mood: CompanionMood): Int = when (mood) {
    CompanionMood.SAD -> R.string.companion_sad
    CompanionMood.BORED -> R.string.companion_bored
    CompanionMood.INTERESTED -> R.string.companion_interested
    CompanionMood.HAPPY -> R.string.companion_happy
    CompanionMood.CELEBRATING -> R.string.companion_celebrating
}
