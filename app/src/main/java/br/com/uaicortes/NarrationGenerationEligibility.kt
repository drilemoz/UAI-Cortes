package br.com.uaicortes

/** Explica por que a geração ainda não pode começar, sem modificar estado nem gastar cota. */
internal object NarrationGenerationEligibility {
    fun blockingReasons(
        busy: Boolean,
        hasVoice: Boolean,
        catalogEmpty: Boolean,
        syncedMode: Boolean,
        syncValidation: String?,
        hasBlankSegment: Boolean,
        hasContinuousText: Boolean,
        remainingMs: Long
    ): List<String> {
        if (busy) return listOf("Uma operação está em andamento.")
        val reasons = mutableListOf<String>()
        if (!hasVoice) reasons += if (catalogEmpty)
            "Consulte o catálogo de vozes e escolha uma voz."
        else "Escolha uma voz para a narração."
        if (syncedMode) {
            if (syncValidation != null) reasons += syncValidation
            if (hasBlankSegment && syncValidation == null) reasons += "Há uma fala sem texto. Revise o trecho."
        } else {
            if (!hasContinuousText) reasons += "Informe o texto da narração."
            if (remainingMs < 250L) reasons += "Não há espaço para áudio no cursor. Mova o cursor do vídeo."
        }
        return reasons
    }
}
