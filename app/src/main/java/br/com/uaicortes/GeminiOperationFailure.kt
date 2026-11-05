package br.com.uaicortes

/** Diagnóstico categorizado sem armazenar corpo HTTP, prompt ou credencial. */
internal enum class GeminiFailureKind { RATE_LIMIT, AUTHORIZATION, BILLING, MODEL,
    INVALID_REQUEST, REMOTE_UNAVAILABLE, NETWORK_UNCERTAIN, RESPONSE_INVALID, OTHER }

internal object GeminiOperationFailure {
    fun fromHttp(code: Int): GeminiFailureKind = when (code) {
        429 -> GeminiFailureKind.RATE_LIMIT
        401, 403 -> GeminiFailureKind.AUTHORIZATION
        402 -> GeminiFailureKind.BILLING
        404 -> GeminiFailureKind.MODEL
        400, 413, 422 -> GeminiFailureKind.INVALID_REQUEST
        408, 500, 502, 503, 504 -> GeminiFailureKind.REMOTE_UNAVAILABLE
        else -> GeminiFailureKind.OTHER
    }

    fun fromThrowable(error: Throwable): GeminiFailureKind = when (error) {
        is GeminiHttpException -> fromHttp(error.statusCode)
        is GeminiApiError -> fromHttp(error.http)
        is java.net.SocketTimeoutException, is java.net.UnknownHostException,
        is java.net.ConnectException -> GeminiFailureKind.NETWORK_UNCERTAIN
        is java.io.IOException -> GeminiFailureKind.NETWORK_UNCERTAIN
        is IllegalArgumentException -> GeminiFailureKind.INVALID_REQUEST
        else -> GeminiFailureKind.RESPONSE_INVALID
    }

    fun label(kind: GeminiFailureKind): String = when (kind) {
        GeminiFailureKind.RATE_LIMIT -> "Limite 429: aguarde a cota disponível"
        GeminiFailureKind.AUTHORIZATION -> "Chave sem autorização: revise o projeto"
        GeminiFailureKind.BILLING -> "Verifique faturamento do projeto"
        GeminiFailureKind.MODEL -> "Modelo indisponível para o projeto"
        GeminiFailureKind.INVALID_REQUEST -> "Solicitação inválida: revise os dados"
        GeminiFailureKind.REMOTE_UNAVAILABLE -> "Serviço temporariamente indisponível"
        GeminiFailureKind.NETWORK_UNCERTAIN -> "Resposta incerta: a geração pode ter consumido cota"
        GeminiFailureKind.RESPONSE_INVALID -> "Resposta recebida não pôde ser utilizada"
        GeminiFailureKind.OTHER -> "Erro da API não classificado"
    }
}
