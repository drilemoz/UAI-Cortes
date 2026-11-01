package br.com.uaicortes

/** Pesquisa por rótulo traduzido, nome original, ID e metadados fornecidos pela API. */
internal object GeminiVoiceSearch {
    fun filter(voices: List<GeminiVoice>, query: String): List<GeminiVoice> {
        val target = GeminiVoicePresentation.fold(query.trim())
        if (target.isEmpty()) return voices
        return voices.filter { GeminiVoicePresentation.fold(GeminiVoicePresentation.searchableText(it)).contains(target) }
    }
}
