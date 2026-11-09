package br.com.uaicortes

/** Orientação de idioma da fala Google. Não traduz nem altera o texto enviado. */
internal object GeminiSpeechLanguage {
    const val PORTUGUESE_BR = "pt-BR"
    const val AUTOMATIC = "auto"
    val choices = listOf(PORTUGUESE_BR, AUTOMATIC)

    fun label(mode: String): String = when (mode) {
        PORTUGUESE_BR -> "Português (Brasil)"
        AUTOMATIC -> "Detectar pelo texto"
        else -> "Detectar pelo texto"
    }

    /** Modelo recebe uma orientação de interpretação, nunca uma promessa de sotaque. */
    fun style(base: String, mode: String): String {
        require(mode in choices) { "Idioma de fala inválido." }
        val clean = base.trim()
        if (mode == AUTOMATIC) return clean
        val direction = "Speak the supplied text in Brazilian Portuguese (pt-BR), " +
            "with natural Brazilian pronunciation and rhythm. Do not translate, " +
            "add, omit, or rewrite any words. Preserve the selected voice identity."
        return if (clean.isEmpty()) direction else "$clean. $direction"
    }
}
