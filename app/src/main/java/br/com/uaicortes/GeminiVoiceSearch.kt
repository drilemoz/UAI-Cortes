package br.com.uaicortes

import java.text.Normalizer
import java.util.Locale

/** Pesquisa leve por nome e ID reais da API, sem alterar os identificadores enviados ao Gemini. */
internal object GeminiVoiceSearch {
    private fun folded(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)

    fun filter(voices: List<GeminiVoice>, query: String): List<GeminiVoice> {
        val target = folded(query.trim())
        if (target.isEmpty()) return voices
        return voices.filter { folded(it.name).contains(target) || folded(it.id).contains(target) }
    }
}
