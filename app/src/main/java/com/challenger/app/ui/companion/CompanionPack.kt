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

        // Расширение подбираем сами: пак переживает замену jpg на png без правки манифеста.
        return listOf(filled, bare)
            .flatMap { name -> withKnownExtensions(name) }
            .map { "$ASSET_ROOT/$id/$it" }
            .firstOrNull { exists(context, it) }
    }

    private fun withKnownExtensions(name: String): List<String> {
        val stem = name.substringBeforeLast('.', name)
        return listOf(name) + EXTENSIONS.map { "$stem.$it" }
    }

    /** Есть ли в паке хоть один файл, который реально лежит в assets. */
    fun hasAnyFrame(context: Context): Boolean =
        CompanionMood.entries.any { assetFor(context, it, emptyMap()) != null }

    /** Нет кадра для настроения — берём ближайшее по смыслу. */
    private fun fallbackMood(mood: CompanionMood): CompanionMood = when (mood) {
        CompanionMood.PRAISE -> CompanionMood.CELEBRATING
        CompanionMood.CELEBRATING -> CompanionMood.HAPPY
        CompanionMood.HAPPY -> CompanionMood.NEUTRAL
        CompanionMood.SAD -> CompanionMood.WAITING
        CompanionMood.WAITING -> CompanionMood.NEUTRAL
        CompanionMood.NEUTRAL -> CompanionMood.WAITING
    }

    companion object {
        const val ASSET_ROOT = "companion"
        private val EXTENSIONS = listOf("webp", "png", "jpg", "json")
        // На Android регулярки строже, чем на JVM: закрывающую скобку
        // обязательно экранировать, иначе PatternSyntaxException при загрузке класса.
        private val PLACEHOLDER = Regex("""\{(\w+)\}""")

        private fun exists(context: Context, path: String): Boolean =
            runCatching { context.assets.open(path).close(); true }.getOrDefault(false)
    }
}

object CompanionPacks {

    private const val TAG = "CompanionPacks"

    /**
     * Паки, у которых есть хотя бы один настоящий кадр. Папку с одним pack.json
     * и без картинок не показываем: иначе настройки врут, что графика уже есть.
     */
    fun available(context: Context): List<CompanionPack> =
        runCatching {
            context.assets.list(CompanionPack.ASSET_ROOT)
                .orEmpty()
                .mapNotNull { load(context, it) }
                .filter { pack -> pack.hasAnyFrame(context) }
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
