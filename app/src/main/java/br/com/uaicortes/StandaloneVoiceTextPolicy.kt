package br.com.uaicortes

/** Estimativa informativa, não equivale ao countTokens nem à cobrança da API. */
internal object StandaloneVoiceTextPolicy {
    const val MAX_CHARS = 4000
    fun length(text: String): Int = text.codePointCount(0, text.length)
    fun accepted(text: String, voiceId: String): Boolean = text.isNotBlank() &&
        text.length <= MAX_CHARS && length(text) <= MAX_CHARS && voiceId.isNotBlank()
    fun tokenEstimate(text: String): Int = if (text.isBlank()) 0 else (length(text) + 3) / 4
}
