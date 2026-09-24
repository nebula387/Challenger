package com.challenger.app.ui.companion

import android.content.Context
import android.util.Log
import com.challenger.app.domain.CompanionMood
import org.json.JSONObject

/**
 * Набор картинок и анимаций спутницы. Лежит в assets/companion/<id>/ и описывается
 * файлом pack.json, поэтому художник добавляет новый образ без единой строчки кода.
 *
 * pack.json:
 * {
 *   "name": "Ann",
 *   "options": { "hair": ["long", "short"], "outfit": ["casual", "sport"] },
 *   "frames": {
 *     "BORED": "{hair}_{outfit}_bored.webp",
 *     "CELEBRATING": "{hair}_{outfit}_dance.json"
 *   }
 * }
 *
 * Значение frames — имя файла внутри той же папки. Расширение .json читается как
 * Lottie-анимация, всё остальное — как картинка.
 */
data class CompanionPack(
    val id: String,
    val name: String,
    val options: Map<String, List<String>>,
    private val frames: Map<CompanionMood, String>
) {
    /**
     * Путь к файлу для настроения с учётом выбранных вариантов.
     * Если варианта нет, пробуем шаблон без подстановок — так пак может быть
     * и одним набором кадров без всякой кастомизации.
     */
    fun assetFor(
        context: Context,
        mood: CompanionMood,
        selected: Map<String, String>
    ): String? {
        val template = frames[mood] ?: frames[fallbackMood(mood)] ?: return null

        val filled = template.replace(PLACEHOLDER) { match ->
            val key = match.groupValues[1]
            selected[key] ?: options[key]?.firstOrNull().orEmpty()
        }
        val bare = template.replace(PLACEHOLDER, "").replace("__", "_").trimStart('_')

        return listOf(filled, bare)
            .map { "$ASSET_ROOT/$id/$it" }
            .firstOrNull { exists(context, it) }
    }

    /** Нет кадра для настроения — берём ближайшее по смыслу. */
    private fun fallbackMood(mood: CompanionMood): CompanionMood = when (mood) {
        CompanionMood.SAD -> CompanionMood.BORED
        CompanionMood.CELEBRATING -> CompanionMood.HAPPY
        CompanionMood.HAPPY -> CompanionMood.INTERESTED
        else -> CompanionMood.BORED
    }

    companion object {
        const val ASSET_ROOT = "companion"
        private val PLACEHOLDER = Regex("""\{(\w+)}""")

        private fun exists(context: Context, path: String): Boolean =
            runCatching { context.assets.open(path).close(); true }.getOrDefault(false)
    }
}

object CompanionPacks {

    private const val TAG = "CompanionPacks"

    /** Все паки, найденные в assets/companion. Пустой список — рисуем встроенную заглушку. */
    fun available(context: Context): List<CompanionPack> =
        runCatching {
            context.assets.list(CompanionPack.ASSET_ROOT)
                .orEmpty()
                .mapNotNull { load(context, it) }
        }.getOrElse {
            Log.w(TAG, "Не удалось прочитать паки спутницы", it)
            emptyList()
        }

    fun load(context: Context, id: String): CompanionPack? = runCatching {
        val json = context.assets
            .open("${CompanionPack.ASSET_ROOT}/$id/pack.json")
            .bufferedReader()
            .use { it.readText() }

        val root = JSONObject(json)

        val options = root.optJSONObject("options")?.let { obj ->
            obj.keys().asSequence().associateWith { key ->
                obj.getJSONArray(key).let { arr -> List(arr.length()) { arr.getString(it) } }
            }
        }.orEmpty()

        val frames = root.optJSONObject("frames")?.let { obj ->
            CompanionMood.entries.mapNotNull { mood ->
                obj.optString(mood.name).takeIf { it.isNotBlank() }?.let { mood to it }
            }.toMap()
        }.orEmpty()

        CompanionPack(
            id = id,
            name = root.optString("name", id),
            options = options,
            frames = frames
        )
    }.getOrNull()
}
