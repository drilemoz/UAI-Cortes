package br.com.uaicortes

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import javax.net.ssl.SSLException

internal enum class SubtitleCloudPhase(val label: String) {
    AUDIO("Preparar áudio"), KEY("Ler chave"), UPLOAD_START("Iniciar envio"),
    UPLOAD_BYTES("Enviar áudio"), PROCESS_FILE("Processar áudio no Gemini"),
    TRANSCRIBE("Gerar transcrição"), READ_RESULT("Ler transcrição"),
    PROOFREAD("Revisar português")
}

internal enum class SubtitleCloudReason {
    HTTP, TIMEOUT, NO_CONNECTION, TLS, IO, INVALID_RESULT, FILE_PROCESSING, LOCAL
}

internal data class SubtitleCloudIssue(
    val phase: SubtitleCloudPhase, val reason: SubtitleCloudReason, val http: Int? = null
) {
    /** Only an interrupted request or a rejected server operation can have an unknown outcome. */
    val needsRecoveryReview: Boolean get() =
        phase == SubtitleCloudPhase.TRANSCRIBE && (reason == SubtitleCloudReason.TIMEOUT ||
            reason == SubtitleCloudReason.IO || http == 408 || (http ?: 0) in 500..599)

    fun message(): String = when (http ?: 0) {
        400, 413, 422 -> if (phase == SubtitleCloudPhase.PROOFREAD)
            "O modelo recusou a revisão de texto. Tente com um bloco menor."
            else "O Gemini recusou o pedido. Confira a fonte e tente com um trecho curto."
        401, 403 -> "A chave ou o projeto não tem autorização. Confira em Configurações do Gemini."
        402 -> "Confira o faturamento do projeto Gemini."
        404 -> if (phase == SubtitleCloudPhase.TRANSCRIBE || phase == SubtitleCloudPhase.PROOFREAD)
            "Modelo indisponível para esta chave. Confira o projeto do Gemini."
            else "O arquivo ou serviço de envio não foi encontrado."
        429 -> "Cota ou limite de solicitações atingido. Aguarde ou confira o projeto Gemini."
        in 500..599 -> "O serviço Gemini respondeu com falha temporária."
        else -> when (reason) {
            SubtitleCloudReason.TIMEOUT -> "A resposta demorou além do limite."
            SubtitleCloudReason.NO_CONNECTION -> "Não foi possível conectar ao Google. Confira a internet."
            SubtitleCloudReason.TLS -> "Não foi possível abrir uma conexão segura com o Google."
            SubtitleCloudReason.INVALID_RESULT -> if (phase == SubtitleCloudPhase.READ_RESULT)
                "O Gemini não devolveu uma transcrição com horários utilizáveis."
                else "O Gemini devolveu uma resposta que o app não conseguiu ler."
            SubtitleCloudReason.FILE_PROCESSING -> "O Gemini não conseguiu disponibilizar o áudio para transcrever."
            SubtitleCloudReason.LOCAL -> if (phase == SubtitleCloudPhase.KEY)
                "Configure uma chave Gemini válida." else "Não foi possível preparar o áudio. Confira a fonte ou escolha outro trecho."
            SubtitleCloudReason.HTTP -> "O Gemini recusou a operação."
            SubtitleCloudReason.IO -> if (phase == SubtitleCloudPhase.AUDIO)
                "Não foi possível ler ou preparar o áudio no aparelho."
                else "A comunicação com o Gemini foi interrompida."
        }
    }

    fun diagnostic(): String =
        "step=${phase.name.lowercase(Locale.ROOT)} reason=${reason.name.lowercase(Locale.ROOT)}" +
            (http?.takeIf { it in 400..599 }?.let { " http=$it" } ?: "")

    companion object {
        fun from(phase: SubtitleCloudPhase, failure: Throwable): SubtitleCloudIssue {
            if (failure is SubtitleCloudException) return failure.issue
            return when (failure) {
                is GeminiHttpException -> SubtitleCloudIssue(phase, SubtitleCloudReason.HTTP, failure.statusCode)
                is GeminiApiError -> SubtitleCloudIssue(phase, SubtitleCloudReason.HTTP, failure.http)
                is SocketTimeoutException -> SubtitleCloudIssue(phase, SubtitleCloudReason.TIMEOUT)
                is UnknownHostException, is ConnectException -> SubtitleCloudIssue(phase, SubtitleCloudReason.NO_CONNECTION)
                is SSLException -> SubtitleCloudIssue(phase, SubtitleCloudReason.TLS)
                is org.json.JSONException -> SubtitleCloudIssue(phase, SubtitleCloudReason.INVALID_RESULT)
                is IOException -> SubtitleCloudIssue(phase, SubtitleCloudReason.IO)
                else -> SubtitleCloudIssue(phase, if (phase == SubtitleCloudPhase.READ_RESULT)
                    SubtitleCloudReason.INVALID_RESULT else SubtitleCloudReason.LOCAL)
            }
        }
    }
}

internal class SubtitleCloudException(val issue: SubtitleCloudIssue) : IOException(issue.message())
