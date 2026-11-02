package br.com.uaicortes

/** Confirma somente uma voz existente no catálogo e nunca dispara chamadas à API. */
internal object GeminiVoiceChoice {
    fun confirmed(catalog: List<GeminiVoice>, draftId: String?): GeminiVoice? =
        catalog.firstOrNull { it.id == draftId }
}
