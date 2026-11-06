package br.com.uaicortes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** Catálogo público compartilhado entre a Central, Vozes IA e narração; nunca guarda chaves. */
internal class GeminiVoiceLibrary(
    private val directory: File,
    private val prefs: android.content.SharedPreferences
) {
    constructor(context: Context) : this(
        File(context.applicationContext.filesDir, "gemini_voice_library"),
        context.applicationContext.getSharedPreferences("gemini_voice_library_settings", Context.MODE_PRIVATE))
    private val file = File(directory, "catalog.json")

    internal data class Snapshot(val voices: List<GeminiVoice>, val updatedAt: Long)

    /** Desligado por padrão: entrar em uma tela nunca precisa fazer GET /voices. */
    fun autoRefresh(): Boolean = prefs.getBoolean("auto_refresh", false)
    fun setAutoRefresh(value: Boolean): Boolean = prefs.edit().putBoolean("auto_refresh", value).commit()
    fun limitMb(): Int = prefs.getInt("preview_limit_mb", 100).takeIf { it in listOf(50, 100, 250) } ?: 100
    fun setLimitMb(value: Int): Boolean = value in listOf(50, 100, 250) &&
        prefs.edit().putInt("preview_limit_mb", value).commit()

    /** Ler fora da thread principal; dados incompletos não substituem a lista anterior. */
    fun load(): Snapshot {
        if (!file.isFile || file.length() !in 2L..MAX_JSON_BYTES) return Snapshot(emptyList(), 0L)
        return runCatching {
            val data = JSONObject(file.readText(Charsets.UTF_8))
            if (data.optInt("schema") != 1) return@runCatching Snapshot(emptyList(), 0L)
            val array = data.optJSONArray("voices") ?: return@runCatching Snapshot(emptyList(), 0L)
            if (array.length() > MAX_VOICES) return@runCatching Snapshot(emptyList(), 0L)
            val voices = buildList {
                for (i in 0 until array.length()) {
                    val entry = array.optJSONObject(i) ?: continue
                    val id = entry.optString("id")
                    if (!GeminiVoiceFavorites.valid(id)) continue
                    add(GeminiVoice(id, entry.optString("name").take(120),
                        entry.optString("description").take(300), entry.optString("language").take(30),
                        entry.optString("accent").take(80), entry.optString("persona").take(100),
                        entry.optString("context").take(80), entry.optString("gender").take(30),
                        entry.optString("pitch").take(30)))
                }
            }.distinctBy { it.id }
            Snapshot(voices, data.optLong("updatedAt").coerceAtLeast(0L))
        }.getOrDefault(Snapshot(emptyList(), 0L))
    }

    /** Só grava resposta utilizável. Falha de rede ou resposta vazia preservam o snapshot. */
    fun save(voices: List<GeminiVoice>): Snapshot {
        val cleaned = voices.filter { GeminiVoiceFavorites.valid(it.id) }.distinctBy { it.id }
        require(cleaned.isNotEmpty() && cleaned.size <= MAX_VOICES) { "Catálogo vazio ou excessivo: não foi substituído." }
        val now = System.currentTimeMillis()
        val array = JSONArray()
        cleaned.forEach { v -> array.put(JSONObject()
            .put("id", v.id).put("name", v.name.take(120)).put("description", v.description.take(300))
            .put("language", v.languageCode.take(30)).put("accent", v.accent.take(80))
            .put("persona", v.persona.take(100)).put("context", v.context.take(80))
            .put("gender", v.gender.take(30)).put("pitch", v.pitch.take(30))) }
        val bytes = JSONObject().put("schema", 1).put("updatedAt", now)
            .put("voices", array).toString().toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_JSON_BYTES) throw IOException("Catálogo maior que o limite local.")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Pasta de catálogo indisponível.")
        val temporary = File(directory, "catalog.json.part")
        try {
            temporary.outputStream().use { it.write(bytes); it.fd.sync() }
            if (!temporary.renameTo(file)) throw IOException("Não foi possível salvar catálogo.")
        } finally { temporary.delete() }
        return Snapshot(cleaned, now)
    }

    companion object {
        const val MAX_VOICES = 500
        const val MAX_JSON_BYTES = 1024L * 1024
    }
}
