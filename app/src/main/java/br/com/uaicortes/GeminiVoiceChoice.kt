package br.com.uaicortes

/** Confirma somente uma voz existente no catálogo e nunca dispara chamadas à API. */
internal object GeminiVoiceChoice {
    fun confirmed(catalog: List<GeminiVoice>, draftId: String?): GeminiVoice? =
        catalog.firstOrNull { it.id == draftId }

    /** Não confirma uma voz antiga que esteja oculta pela busca ou filtro atual. */
    fun visibleChoice(visible: List<GeminiVoice>, draftId: String?): GeminiVoice? =
        visible.firstOrNull { it.id == draftId }

    /** Ao mudar de filtro, nenhuma voz oculta pode continuar marcada. */
    fun visibleIdOrNull(visible: List<GeminiVoice>, draftId: String?): String? =
        visibleChoice(visible, draftId)?.id
}
