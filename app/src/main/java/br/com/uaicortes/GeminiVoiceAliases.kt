package br.com.uaicortes

import android.content.Context
import org.json.JSONObject

/** Apelidos privados e opcionais. Nunca alteram IDs usados nas chamadas Gemini. */
internal class GeminiVoiceAliases(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gemini_voice_aliases", Context.MODE_PRIVATE)

    fun load(): Map<String, String> = runCatching {
        val raw = prefs.getString("aliases", "{}") ?: "{}"
        if (raw.length > 32_768) return@runCatching emptyMap()
        val objectValue = JSONObject(raw)
        buildMap {
            val keys = objectValue.keys()
            while (keys.hasNext() && size < MAX_ALIASES) {
                val id = keys.next()
                val alias = objectValue.optString(id)
                if (GeminiVoiceFavorites.valid(id) && validAlias(alias)) put(id, alias)
            }
        }
    }.getOrDefault(emptyMap())

    fun save(id: String, alias: String): Map<String, String>? {
        if (!GeminiVoiceFavorites.valid(id)) return null
        val value = alias.trim()
        if (value.isNotEmpty() && !validAlias(value)) return null
        val previous = load()
        if (value.isNotEmpty() && id !in previous && previous.size >= MAX_ALIASES) return null
        val updated = if (value.isBlank()) previous - id else previous + (id to value)
        val json = JSONObject()
        updated.forEach { (key, name) -> json.put(key, name) }
        return updated.takeIf { prefs.edit().putString("aliases", json.toString()).commit() }
    }

    companion object {
        const val MAX_ALIASES = 200
        const val MAX_ALIAS_CHARS = 50
        fun validAlias(value: String): Boolean = value.length in 1..MAX_ALIAS_CHARS &&
            value.isNotBlank() && value.none { it.isISOControl() }
        fun title(voice: GeminiVoice, aliases: Map<String, String>): String =
            aliases[voice.id]?.takeIf { validAlias(it) } ?: GeminiVoicePresentation.label(voice)
    }
}
