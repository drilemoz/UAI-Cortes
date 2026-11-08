package br.com.uaicortes

/** Pesquisa por rótulo traduzido, nome original, ID e metadados fornecidos pela API. */
internal object GeminiVoiceSearch {
    /** Filtro baseado exclusivamente no campo gender retornado pela API. */
    fun byGender(voices: List<GeminiVoice>, filter: String): List<GeminiVoice> = when (filter) {
        "Femininas" -> voices.filter { it.gender.equals("female", true) }
        "Masculinas" -> voices.filter { it.gender.equals("male", true) }
        else -> voices
    }

    /** Nunca inferir a altura a partir do nome: só filtrar quando a API fornece pitch. */
    fun byPitch(voices: List<GeminiVoice>, filter: String): List<GeminiVoice> = when (filter) {
        "Graves" -> voices.filter { it.pitch.equals("low", true) }
        "Médias" -> voices.filter { it.pitch.equals("medium", true) }
        "Agudas" -> voices.filter { it.pitch.equals("high", true) }
        "Não informado" -> voices.filter { it.pitch.isBlank() }
        else -> voices
    }

    /** Valores reais do catálogo. Campos vazios ou desconhecidos não viram classificações fictícias. */
    fun values(voices: List<GeminiVoice>, field: String): List<String> = voices.mapNotNull { voice ->
        val value = fieldValue(voice, field).trim()
        value.takeIf { it.isNotEmpty() }
    }.distinct().sortedBy { GeminiVoicePresentation.fold(it) }

    fun byMetadata(voices: List<GeminiVoice>, field: String, value: String): List<GeminiVoice> =
        if (value.isBlank()) voices else voices.filter { fieldValue(it, field).equals(value, true) }

    private fun fieldValue(voice: GeminiVoice, field: String): String = when (field) {
        "language" -> voice.languageCode
        "accent" -> voice.accent
        "persona" -> voice.persona
        "context" -> voice.context
        else -> ""
    }

    /** Inclui apelidos locais sem contaminar a busca pública nem alterar o ID da API. */
    fun filterWithAliases(voices: List<GeminiVoice>, query: String,
                          aliases: Map<String, String>): List<GeminiVoice> {
        val target = GeminiVoicePresentation.fold(query.trim())
        if (target.isEmpty()) return voices
        return voices.filter { GeminiVoicePresentation.fold(
            GeminiVoicePresentation.searchableText(it) + " " + aliases[it.id].orEmpty()).contains(target) }
    }

    fun filter(voices: List<GeminiVoice>, query: String): List<GeminiVoice> {
        val target = GeminiVoicePresentation.fold(query.trim())
        if (target.isEmpty()) return voices
        return voices.filter { GeminiVoicePresentation.fold(GeminiVoicePresentation.searchableText(it)).contains(target) }
    }
}
