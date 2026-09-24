package com.challenger.app.ui.companion

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.challenger.app.data.prefs.CompanionSettings
import com.challenger.app.domain.CompanionMood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Спутница на главном экране. Порядок такой: если в assets лежит пак с картинками
 * или Lottie-анимациями — показываем его, иначе рисуем встроенную заглушку,
 * чтобы экран работал сразу, ещё до того как появится настоящая графика.
 */
@Composable
fun Companion(
    mood: CompanionMood,
    settings: CompanionSettings,
    modifier: Modifier = Modifier
) {
    if (!settings.enabled) return

    val context = LocalContext.current
    val pack by produceState<CompanionPack?>(initialValue = null, settings.packId) {
        value = withContext(Dispatchers.IO) { CompanionPacks.load(context, settings.packId) }
    }

    val asset by produceState<String?>(initialValue = null, pack, mood, settings) {
        value = withContext(Dispatchers.IO) {
            pack?.assetFor(context, mood, settings.options())
        }
    }

    Box(
        modifier = modifier.alpha(settings.opacity),
        contentAlignment = Alignment.BottomCenter
    ) {
        val path = asset
        when {
            path == null -> PlaceholderCompanion(mood, Modifier.fillMaxSize())
            path.endsWith(".json", ignoreCase = true) -> LottieCompanion(path, mood)
            else -> ImageCompanion(path, mood)
        }
    }
}

@Composable
private fun LottieCompanion(assetPath: String, mood: CompanionMood) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        // Радость играется живее, ожидание — спокойно.
        speed = if (mood == CompanionMood.CELEBRATING) 1.2f else 0.8f
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ImageCompanion(assetPath: String, mood: CompanionMood) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, assetPath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }

    val image = bitmap ?: return

    // Статичной картинке добавляем дыхание, иначе она выглядит мёртвой.
    val transition = rememberInfiniteTransition(label = "breath")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (mood.isPositive) 1.035f else 1.012f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (mood == CompanionMood.CELEBRATING) 700 else 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .scale(scale)
    )
}
