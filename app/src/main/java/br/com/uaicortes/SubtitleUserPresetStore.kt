package br.com.uaicortes

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** Biblioteca privada do aparelho, compartilhada entre projetos e independente do JSON dos vídeos. */
internal class SubtitleUserPresetStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("subtitle_user_presets", Context.MODE_PRIVATE)

    suspend fun read(): List<SubtitleUserPreset> = withContext(Dispatchers.IO) {
        decode(prefs.getString("presets_v1", null))
    }

    /** Grava de forma atômica; não anuncia sucesso em caso de falha do armazenamento. */
    suspend fun write(values: List<SubtitleUserPreset>) = withContext(Dispatchers.IO + NonCancellable) {
        val encoded = encode(values)
        if (!prefs.edit().putString("presets_v1", encoded).commit()) {
            throw IOException("Não foi possível salvar os modelos neste aparelho.")
        }
    }

    companion object {
        private val STYLE_KEYS = arrayOf(
            "color", "sizeSp", "backgroundColor", "bold", "fontFamily", "outlineWidth",
            "outlineColor", "shadowOffsetDp", "shadowColor", "lineSpacingPercent",
            "textAlignment", "animation", "animationSpeed", "highlightColor", "italic"
        )

        /** Persiste apenas propriedades visuais, nunca IDs/textos/horários/posição da legenda. */
        internal fun encode(values: List<SubtitleUserPreset>): String {
            require(values.size <= SubtitleUserPresets.MAX_ITEMS)
            require(values.map { it.id }.distinct().size == values.size)
            val names = mutableListOf<SubtitleUserPreset>()
            val array = JSONArray()
            for (preset in values) {
                require(preset.id.isNotBlank() && preset.id.length <= 80)
                require(SubtitleUserPresets.nameError(preset.name, names) == null)
                val full = SubtitleUserPresets.snapshot(preset.sample).json()
                val visual = JSONObject()
                for (key in STYLE_KEYS) visual.put(key, full.get(key))
                array.put(JSONObject().put("id", preset.id).put("name", preset.name)
                    .put("style", visual))
                names += preset
            }
            return JSONObject().put("schema", 1).put("presets", array).toString()
        }

        internal fun decode(raw: String?): List<SubtitleUserPreset> {
            if (raw.isNullOrBlank()) return emptyList()
            require(raw.length <= 120_000) { "Biblioteca de modelos excede o tamanho permitido." }
            val root = JSONObject(raw)
            require(root.getInt("schema") == 1) { "Versão da biblioteca não reconhecida." }
            val array = root.getJSONArray("presets")
            require(array.length() <= SubtitleUserPresets.MAX_ITEMS) { "Biblioteca de modelos excede o limite." }
            val result = mutableListOf<SubtitleUserPreset>()
            repeat(array.length()) { index ->
                val objectValue = array.getJSONObject(index)
                val id = objectValue.getString("id")
                val name = objectValue.getString("name")
                require(id.isNotBlank() && id.length <= 80 && result.none { it.id == id }) {
                    "Identificação duplicada ou inválida."
                }
                require(SubtitleUserPresets.nameError(name, result) == null &&
                    SubtitleUserPresets.normalizedName(name) == name) { "Nome de modelo inválido." }
                val visual = objectValue.getJSONObject("style")
                // Exigir todas as chaves evita aceitar silenciosamente uma biblioteca danificada.
                require(STYLE_KEYS.all(visual::has)) { "Estilo incompleto ou danificado." }
                val cue = JSONObject().put("text", "").put("startMs", 0).put("endMs", 1)
                for (key in STYLE_KEYS) cue.put(key, visual.get(key))
                result += SubtitleUserPreset(id, name,
                    SubtitleUserPresets.snapshot(SubtitleCue.read(cue)))
            }
            return result
        }
    }
}
