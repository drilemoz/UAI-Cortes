package br.com.uaicortes

/** Estimativa informativa, não equivale ao countTokens nem à cobrança da API. */
internal object StandaloneVoiceTextPolicy {
    const val MAX_CHARS = 4000
    fun length(text: String): Int = text.codePointCount(0, text.length)
    fun accepted(text: String, voiceId: String): Boolean = text.isNotBlank() &&
        text.length <= MAX_CHARS && length(text) <= MAX_CHARS && voiceId.isNotBlank()
    fun tokenEstimate(text: String): Int = if (text.isBlank()) 0 else (length(text) + 3) / 4
    /** Faixa ilustrativa a 120–170 palavras/minuto, sem prever a saída do Gemini. */
    fun durationRangeSeconds(text: String): IntRange {
        val words = Regex("\\S+").findAll(text.trim()).count()
        if (words == 0) return 0..0
        return ((words * 60 + 169) / 170)..((words * 60 + 119) / 120)
    }
}
