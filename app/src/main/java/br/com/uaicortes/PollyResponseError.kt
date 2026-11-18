package br.com.uaicortes

import org.json.JSONObject
import java.io.IOException

/** Map known machine codes to local messages. Never return the raw AWS error body or message. */
internal object PollyResponseError {
    private enum class Kind(val explanation: String) {
        CREDENTIALS("A AWS não reconheceu as credenciais. Use o ID e a chave secreta do mesmo par ativo."),
        TOKEN("Token de sessão ausente, inválido ou expirado. Se a credencial for temporária, renove o conjunto completo."),
        IAM("Permissão negada. Confira se o usuário tem a ação do Polly necessária para este teste."),
        CLOCK("A AWS rejeitou o horário da solicitação. Ative data e hora automáticas no Android."),
        SIGNATURE("A assinatura foi rejeitada. Confira o par de chaves, a região e o horário automático do Android."),
        VALIDATION("A AWS rejeitou um parâmetro da solicitação. Isso, por si só, não indica chave inválida."),
        VOICE("A voz e o tipo de áudio escolhido não são compatíveis nessa região. Selecione outra voz ou outro tipo."),
        QUOTA("A AWS limitou as chamadas. Aguarde um pouco antes de repetir."),
        SERVER("O Polly não conseguiu processar a solicitação agora. Tente novamente mais tarde.")
    }

    private val known = mapOf(
        "InvalidAccessKeyId" to Kind.CREDENTIALS,
        "InvalidClientTokenId" to Kind.CREDENTIALS,
        "UnrecognizedClientException" to Kind.CREDENTIALS,
        "ExpiredToken" to Kind.TOKEN,
        "ExpiredTokenException" to Kind.TOKEN,
        "InvalidToken" to Kind.TOKEN,
        "InvalidTokenException" to Kind.TOKEN,
        "AccessDenied" to Kind.IAM,
        "AccessDeniedException" to Kind.IAM,
        "NotAuthorizedException" to Kind.IAM,
        "RequestExpired" to Kind.CLOCK,
        "RequestTimeTooSkewed" to Kind.CLOCK,
        "IncompleteSignature" to Kind.SIGNATURE,
        "IncompleteSignatureException" to Kind.SIGNATURE,
        "SignatureDoesNotMatch" to Kind.SIGNATURE,
        "InvalidSignatureException" to Kind.SIGNATURE,
        "ValidationException" to Kind.VALIDATION,
        "SerializationException" to Kind.VALIDATION,
        "InvalidParameterValue" to Kind.VALIDATION,
        "InvalidNextTokenException" to Kind.VALIDATION,
        "EngineNotSupportedException" to Kind.VOICE,
        "LanguageNotSupportedException" to Kind.VOICE,
        "Throttling" to Kind.QUOTA,
        "ThrottlingException" to Kind.QUOTA,
        "TooManyRequestsException" to Kind.QUOTA,
        "ServiceFailureException" to Kind.SERVER,
        "InternalFailure" to Kind.SERVER
    )

    private fun machineCode(candidate: String?): String? =
        candidate?.substringBefore(':')?.substringAfterLast('#')?.trim()
            ?.takeIf { it in known }

    fun exception(action: String, status: Int, typeHeader: String?, body: String?): IOException {
        val json = body?.takeIf { it.isNotBlank() }?.let { runCatching { JSONObject(it) }.getOrNull() }
        val code = machineCode(typeHeader) ?: listOf("__type", "code", "Code", "type")
            .firstNotNullOfOrNull { machineCode(json?.optString(it, "")) }
        val explanation = code?.let { known[it]?.explanation } ?: when (status) {
            400 -> "Solicitação rejeitada sem código de erro reconhecido. O HTTP 400 não confirma problema na chave."
            401, 403 -> "A AWS negou o acesso. Confira as credenciais e as permissões do Polly no IAM."
            429 -> Kind.QUOTA.explanation
            in 500..599 -> Kind.SERVER.explanation
            else -> "A AWS não concluiu a solicitação."
        }
        val detail = code?.let { " ($it)" }.orEmpty()
        return IOException("$action: HTTP $status$detail. $explanation")
    }
}
