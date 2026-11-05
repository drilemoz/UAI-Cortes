package br.com.uaicortes

import java.time.LocalDate

/** Referências estáticas e datadas, conferidas na documentação oficial em 05/11/2026.
 * NÃO consulta faturamento nem detecta o plano da chave. Usar somente Standard pago,
 * com entrada/saída completas, sem cache, priority, batch ou grounding.
 */
internal data class GeminiPublicRate(
    val inputUsdPerMillion: Double,
    val outputUsdPerMillion: Double,
    val verifiedOn: LocalDate,
    val validFrom: LocalDate,
    val validUntil: LocalDate?,
    val sourceUrl: String = "https://ai.google.dev/gemini-api/docs/pricing"
)

internal object GeminiPublicPriceCatalog {
    private val verified = LocalDate.of(2026, 11, 5)
    private val y2027 = LocalDate.of(2027, 1, 1)
    private val y2026end = LocalDate.of(2026, 12, 31)

    fun lookup(model: String, date: LocalDate = LocalDate.now()): GeminiPublicRate? {
        val rates = when (model.trim()) {
            "gemini-3.8-flash-lite-tts" -> listOf(
                GeminiPublicRate(0.50, 6.0, verified, verified, y2026end),
                GeminiPublicRate(1.0, 12.0, verified, y2027, null))
            "gemini-3.8-flash-tts" -> listOf(
                GeminiPublicRate(0.50, 9.0, verified, verified, y2026end),
                GeminiPublicRate(1.0, 18.0, verified, y2027, null))
            "gemini-3.5-transcribe" -> listOf(GeminiPublicRate(2.0, 12.0, verified, verified, null))
            "gemini-2.5-flash-preview-tts" -> listOf(GeminiPublicRate(0.50, 10.0, verified, verified, null))
            "gemini-2.5-pro-preview-tts" -> listOf(GeminiPublicRate(1.0, 20.0, verified, verified, null))
            else -> emptyList()
        }
        // Tarifas sem validade publicada também exigem checagem de frescor; não aplicamos
        // automaticamente após longo período sem verificação.
        return rates.firstOrNull { date >= it.validFrom &&
            (it.validUntil == null || date <= it.validUntil) &&
            !date.isAfter(it.verifiedOn.plusMonths(6)) }
    }

    fun displayName(model: String): String = when (model) {
        "gemini-3.8-flash-lite-tts" -> "Voz Lite 3.8"
        "gemini-3.8-flash-tts" -> "Voz Flash 3.8"
        "gemini-3.5-transcribe" -> "Transcrição 3.5"
        "gemini-2.5-flash-preview-tts" -> "Voz Flash 2.5"
        "gemini-2.5-pro-preview-tts" -> "Voz Pro 2.5"
        else -> model
    }
}
