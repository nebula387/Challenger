package com.challenger.app.ui.companion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.challenger.app.data.prefs.CompanionPrefs
import com.challenger.app.domain.CompanionMood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Портрет спутницы для виджета и шторки.
 *
 * Эти картинки уезжают в чужой процесс через Binder, а он не принимает больше
 * мегабайта на всю транзакцию. Поэтому лицо всегда уменьшается до запрошенного
 * размера, а не отдаётся как есть: исходные 512x512 в ARGB — это ровно тот
 * мегабайт и есть.
 */
object CompanionFaces {

    private const val TAG = "CompanionFaces"

    suspend fun load(context: Context, mood: CompanionMood, sizePx: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val settings = CompanionPrefs(app).settings.first()
            if (!settings.enabled) return@withContext null

            val pack = CompanionPacks.load(app, settings.packId) ?: return@withContext null
            val path = pack.faceFor(app, mood, settings.options()) ?: return@withContext null

            runCatching { decodeScaled(app, path, sizePx) }
                .onFailure { Log.w(TAG, "Не удалось прочитать лицо: $path", it) }
                .getOrNull()
        }

    private fun decodeScaled(context: Context, path: String, sizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }

        val source = maxOf(bounds.outWidth, bounds.outHeight)
        if (source <= 0) return null

        // Сначала грубо, степенью двойки — так декодер не держит в памяти оригинал.
        val options = BitmapFactory.Options().apply {
            inSampleSize = generateSequence(1) { it * 2 }
                .takeWhile { source / it >= sizePx }
                .last()
        }
        val decoded = context.assets.open(path).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        if (maxOf(decoded.width, decoded.height) <= sizePx) return decoded

        val scale = sizePx.toFloat() / maxOf(decoded.width, decoded.height)
        return Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true
        ).also { if (it !== decoded) decoded.recycle() }
    }
}
