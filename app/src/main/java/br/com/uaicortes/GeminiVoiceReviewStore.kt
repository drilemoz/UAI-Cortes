package br.com.uaicortes

import android.content.Context
import org.json.JSONObject

/** JSON pequeno e privado, sem texto de prévia, tokens ou dados biométricos. */
internal class GeminiVoiceReviewStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gemini_voice_manual_reviews", Context.MODE_PRIVATE)

    fun load(): Map<String, String> = runCatching {
        val raw = prefs.getString("pairs", "{}") ?: "{}"
        if (raw.length > 130_000) return@runCatching emptyMap()
        val json = JSONObject(raw)
        buildMap {
            val keys = json.keys()
            while (keys.hasNext() && size < GeminiVoiceDiscovery.MAX_REVIEWS) {
                val key = keys.next()
                val parts = key.split('\u001f')
                val value = json.optString(key)
                if (parts.size == 2 && GeminiVoiceDiscovery.pairKey(parts[0], parts[1]) == key &&
                    value in listOf(GeminiVoiceDiscovery.SIMILAR, GeminiVoiceDiscovery.DIFFERENT)) put(key, value)
            }
        }
    }.getOrDefault(emptyMap())

    fun save(a: String, b: String, value: String?): Map<String, String>? {
        val key = GeminiVoiceDiscovery.pairKey(a, b) ?: return null
        if (value != null && value !in listOf(GeminiVoiceDiscovery.SIMILAR, GeminiVoiceDiscovery.DIFFERENT)) return null
        val current = load()
        if (value != null && key !in current && current.size >= GeminiVoiceDiscovery.MAX_REVIEWS) return null
        val next = if (value == null) current - key else current + (key to value)
        val json = JSONObject()
        next.forEach { (k, v) -> json.put(k, v) }
        return next.takeIf { prefs.edit().putString("pairs", json.toString()).commit() }
    }
}
