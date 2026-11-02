package br.com.uaicortes

/** Pesquisa por rótulo traduzido, nome original, ID e metadados fornecidos pela API. */
internal object GeminiVoiceSearch {
    /** Filtro baseado exclusivamente no campo gender retornado pela API. */
    fun byGender(voices: List<GeminiVoice>, filter: String): List<GeminiVoice> = when (filter) {
        "Femininas" -> voices.filter { it.gender.equals("female", true) }
        "Masculinas" -> voices.filter { it.gender.equals("male", true) }
        else -> voices
    }

    fun filter(voices: List<GeminiVoice>, query: String): List<GeminiVoice> {
        val target = GeminiVoicePresentation.fold(query.trim())
        if (target.isEmpty()) return voices
        return voices.filter { GeminiVoicePresentation.fold(GeminiVoicePresentation.searchableText(it)).contains(target) }
    }
}
