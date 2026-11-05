package br.com.uaicortes

import android.content.Context
import org.json.JSONObject

/** Vozes favoritas locais, compartilhadas entre projetos; IDs não são credenciais. */
internal class GeminiVoiceFavorites(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gemini_voice_favorites", Context.MODE_PRIVATE)

    fun load(): Set<String> = prefs.getStringSet("ids", emptySet()).orEmpty()
        .filterTo(mutableSetOf()) { valid(it) }

    fun save(ids: Set<String>): Boolean = prefs.edit()
        .putStringSet("ids", ids.filterTo(mutableSetOf()) { valid(it) }.take(MAX_FAVORITES).toSet())
        .commit()

    /** Nomes públicos do catálogo, nunca chaves ou conteúdo das falas. */
    fun names(): Map<String, String> = runCatching {
        val json = JSONObject(prefs.getString("labels", "{}") ?: "{}")
        buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val name = json.optString(id)
                if (valid(id) && name.isNotBlank() && name.length <= 120) put(id, name)
            }
        }
    }.getOrDefault(emptyMap())

    fun saveNames(names: Map<String, String>): Boolean {
        val json = JSONObject()
        names.entries.asSequence().filter { valid(it.key) && it.value.isNotBlank() }
            .take(500).forEach { (id, name) -> json.put(id, name.take(120)) }
        return prefs.edit().putString("labels", json.toString()).commit()
    }

    companion object {
        const val MAX_FAVORITES = 200
        fun valid(id: String): Boolean = id.isNotBlank() && id.length <= 150 &&
            id.none { it.isISOControl() }
        fun toggle(ids: Set<String>, id: String): Set<String> {
            if (!valid(id)) return ids
            if (id in ids) return ids - id
            if (ids.size >= MAX_FAVORITES) return ids
            return ids + id
        }
    }
}

/** A API TTS não informa avanço dentro de uma chamada; somente WAVs concluídos contam. */
internal object VoiceIaBatchProgress {
    fun percent(completed: Int, total: Int): Int =
        if (total <= 0) 0 else (completed.coerceIn(0, total).toLong() * 100L / total).toInt()
}
