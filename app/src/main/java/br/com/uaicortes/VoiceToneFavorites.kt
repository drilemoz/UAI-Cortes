package br.com.uaicortes

import android.content.Context
import org.json.JSONObject

/** Três perfis locais: somente parâmetros de áudio, nunca fontes, URLs ou credenciais. */
internal class VoiceToneFavorites(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "voice_tone_favorites_v1", Context.MODE_PRIVATE)

    fun save(slot: Int, tone: VoiceTone) {
        require(slot in 1..3)
        val normalized = tone.safe()
        if (normalized.enabled) {
            preferences.edit().putString("preset_$slot", normalized.json().toString()).apply()
        }
    }

    fun load(slot: Int): VoiceTone? {
        require(slot in 1..3)
        val raw = preferences.getString("preset_$slot", null) ?: return null
        return runCatching { VoiceTone.read(JSONObject(raw)) }
            .getOrNull()?.takeIf { it.enabled }
    }
}
