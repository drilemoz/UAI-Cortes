package br.com.uaicortes

/** Taxonomia fechada para não salvar parâmetros privados em diagnósticos compartilháveis. */
internal object ApiDiagnosticPolicy {
    private val safe = Regex("[A-Za-z0-9_.-]{1,48}")
    private val services = setOf("gemini", "youtube", "tiktok", "meta")
    private val outcomes = setOf("ok", "http_error", "network_uncertain", "invalid_response", "cancelled", "unknown")
    fun httpStatus(status: String): Int? {
        val value = when {
            status.matches(Regex("[1-5][0-9]{2}")) -> status
            status.matches(Regex("erro_[1-5][0-9]{2}")) -> status.removePrefix("erro_")
            else -> return null
        }
        return value.toIntOrNull()?.takeIf { it in 100..599 }
    }
    fun category(code: Int?): String = when (code) {
        401, 403 -> "auth"
        402 -> "billing"
        404 -> "model_or_resource"
        429 -> "rate_limit"
        408, 500, 502, 503, 504 -> "remote_uncertain"
        400, 413, 422 -> "request"
        null -> "not_provided"
        else -> "other"
    }
    fun sanitize(service: String, operation: String, model: String?, status: Int?,
        elapsedMs: Long?, tokens: Long?, outcome: String): String {
        val provider = service.takeIf { it in services } ?: "unknown"
        val action = operation.takeIf { safe.matches(it) } ?: "unknown"
        val modelId = model?.takeIf { safe.matches(it) && it.startsWith("gemini-") } ?: "not_provided"
        val code = status?.takeIf { it in 100..599 }
        val duration = elapsedMs?.takeIf { it in 0..600_000L }?.toString() ?: "unknown"
        val usage = tokens?.takeIf { it in 0..100_000_000_000L }?.toString() ?: "unknown"
        val result = outcome.takeIf { it in outcomes } ?: "unknown"
        return "service=$provider operation=$action model=$modelId http=${code ?: "unknown"}" +
            " category=${category(code)} latency_ms=$duration tokens=$usage result=$result"
    }
}
