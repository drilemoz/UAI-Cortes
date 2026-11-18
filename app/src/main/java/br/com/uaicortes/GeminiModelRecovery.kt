package br.com.uaicortes

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import org.json.JSONException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal enum class GeminiRecoveryRoute(val label: String) {
    PROOFREAD("Corrigir português"), DESCRIPTION("Descrição e hashtags"),
    SCRIPT("Roteiro para narração"), VISUAL("Análise visual de legendas"),
    TRANSCRIBE("Transcrição com horários"), VOICE("Síntese de voz"), BATCH("Envio em lote")
}

internal enum class GeminiRecoveryOutcome(val label: String) {
    OK("Concluída"), HTTP("HTTP retornado pelo serviço"),
    TIMEOUT("Tempo esgotado; resultado não confirmado"), NETWORK("Falha de conexão"),
    TLS("Falha na conexão segura"), INVALID("Resposta inválida"), UNKNOWN("Resultado não confirmado")
}

internal data class GeminiRecoveryFault(
    val outcome: GeminiRecoveryOutcome,
    val http: Int? = null,
    val modelUnavailable: Boolean = false,
    val stage: String? = null
) {
    companion object {
        fun from(error: Throwable): GeminiRecoveryFault = when (error) {
            is GeminiHttpException -> GeminiRecoveryFault(GeminiRecoveryOutcome.HTTP,
                error.statusCode.takeIf { it in 400..599 }, error.modelUnavailable)
            is GeminiApiError -> GeminiRecoveryFault(GeminiRecoveryOutcome.HTTP,
                error.http.takeIf { it in 400..599 })
            is SubtitleCloudException -> {
                val issue = error.issue
                GeminiRecoveryFault(when (issue.reason) {
                    SubtitleCloudReason.HTTP -> GeminiRecoveryOutcome.HTTP
                    SubtitleCloudReason.TIMEOUT -> GeminiRecoveryOutcome.TIMEOUT
                    SubtitleCloudReason.TLS -> GeminiRecoveryOutcome.TLS
                    SubtitleCloudReason.NO_CONNECTION -> GeminiRecoveryOutcome.NETWORK
                    SubtitleCloudReason.INVALID_RESULT -> GeminiRecoveryOutcome.INVALID
                    else -> GeminiRecoveryOutcome.UNKNOWN
                }, issue.http?.takeIf { it in 400..599 }, stage = issue.phase.label)
            }
            is SocketTimeoutException -> GeminiRecoveryFault(GeminiRecoveryOutcome.TIMEOUT)
            is UnknownHostException, is ConnectException -> GeminiRecoveryFault(GeminiRecoveryOutcome.NETWORK)
            is SSLException -> GeminiRecoveryFault(GeminiRecoveryOutcome.TLS)
            is JSONException, is GeminiInvalidResult -> GeminiRecoveryFault(GeminiRecoveryOutcome.INVALID)
            else -> when (error.cause) {
                is SocketTimeoutException -> GeminiRecoveryFault(GeminiRecoveryOutcome.TIMEOUT)
                is UnknownHostException, is ConnectException -> GeminiRecoveryFault(GeminiRecoveryOutcome.NETWORK)
                is SSLException -> GeminiRecoveryFault(GeminiRecoveryOutcome.TLS)
                else -> GeminiRecoveryFault(GeminiRecoveryOutcome.UNKNOWN)
            }
        }
    }
}

internal class GeminiInvalidResult(message: String) : java.io.IOException(message)

internal data class GeminiRecoveryAttempt(val model: String, val fault: GeminiRecoveryFault, val elapsedMs: Long)

internal data class GeminiRecoveryTrace(
    val route: GeminiRecoveryRoute, val attempts: List<GeminiRecoveryAttempt>,
    val trying: String? = null, val complete: Boolean = false
) {
    val failed: Boolean get() = complete && attempts.lastOrNull()?.fault?.outcome?.let {
        it != GeminiRecoveryOutcome.OK
    } == true
    val status: String? get() = when {
        trying != null -> "Tentando outro modelo… $trying"
        complete && !failed && attempts.size > 1 -> "Modelo alternativo usado · ${attempts.last().model}"
        else -> null
    }

    fun report(version: String): String = buildString {
        appendLine("Uai Cortes · diagnóstico Gemini")
        appendLine("Versão: ${version.takeIf { Regex("[0-9]+(\\.[0-9]+){1,3}").matches(it) } ?: "—"}")
        appendLine("Operação: ${route.label}")
        attempts.forEachIndexed { i, attempt ->
            appendLine("${i + 1}. ${GeminiModelRecovery.publicModel(attempt.model)}")
            append("   ${attempt.fault.outcome.label}")
            attempt.fault.http?.let { append(" · HTTP $it") }
            attempt.fault.stage?.let { stage ->
                if (SubtitleCloudPhase.entries.any { it.label == stage }) append(" · $stage")
            }
            appendLine(" · ${attempt.elapsedMs.coerceAtLeast(0)} ms")
        }
        if (failed && attempts.size == 1) appendLine("Não houve tentativa automática com outro modelo.")
    }
}

internal class GeminiRecoveryChannel(
    val publish: suspend (GeminiRecoveryTrace) -> Unit
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<GeminiRecoveryChannel>
}

/** Lista explícita por modalidade. Legendas só podem alternar para um modelo que aceite áudio. */
internal object GeminiModelRecovery {
    const val TEXT_LITE = "gemini-3.5-flash-lite"
    const val TEXT_LITE_BACKUP = "gemini-3.1-flash-lite"
    private val publicIds = setOf(TEXT_LITE, TEXT_LITE_BACKUP, "gemini-3.5-flash", "gemini-3.6-flash",
        "gemini-3.7-flash", "gemini-3.8-flash", "gemini-2.5-flash", "gemini-2.5-flash-lite",
        "gemini-3.5-transcribe", GeminiModels.FLASH, GeminiModels.LITE)

    fun publicModel(id: String): String = id.takeIf { it in publicIds } ?: "modelo configurado"

    fun alternate(route: GeminiRecoveryRoute, original: String, fault: GeminiRecoveryFault): String? {
        if (route == GeminiRecoveryRoute.TRANSCRIBE) {
            return TEXT_LITE.takeIf {
                original == GeminiTranscriptionRequest.MODEL && fault.outcome == GeminiRecoveryOutcome.HTTP &&
                    fault.stage == SubtitleCloudPhase.TRANSCRIBE.label && fault.http in setOf(404, 429, 503)
            }
        }
        // Um timeout ou erro de transporte não informa se o Google aceitou a operação.
        if (fault.outcome != GeminiRecoveryOutcome.HTTP ||
            !(fault.http == 503 || (fault.http == 404 && fault.modelUnavailable))) return null
        return when (route) {
            GeminiRecoveryRoute.VOICE -> GeminiModels.LITE.takeIf { original == GeminiModels.FLASH }
            GeminiRecoveryRoute.BATCH -> TEXT_LITE.takeIf {
                fault.http == 404 && original != TEXT_LITE
            } // nunca recriar lote após timeout ou 5xx
            else -> if (original == TEXT_LITE) TEXT_LITE_BACKUP else TEXT_LITE
        }
    }

    suspend fun <T> execute(route: GeminiRecoveryRoute, primary: String, allowAlternate: Boolean = true,
        pause: suspend () -> Unit = { delay(1000) }, action: suspend (String, Boolean) -> T): T {
        val context = currentCoroutineContext()
        val channel = context[GeminiRecoveryChannel]
        val attempts = mutableListOf<GeminiRecoveryAttempt>()
        suspend fun publish(trying: String? = null, complete: Boolean = false) {
            channel?.publish?.invoke(GeminiRecoveryTrace(route, attempts.toList(), trying, complete))
        }
        var model = primary
        repeat(2) { index ->
            context.ensureActive()
            val started = System.nanoTime()
            fun elapsed() = ((System.nanoTime() - started) / 1_000_000).coerceAtLeast(0)
            val result = try {
                action(model, index == 0)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                context.ensureActive()
                val fault = GeminiRecoveryFault.from(error)
                attempts += GeminiRecoveryAttempt(publicModel(model), fault, elapsed())
                val next = if (index == 0 && allowAlternate) alternate(route, model, fault) else null
                if (next == null || next == model) {
                    publish(complete = true)
                    throw error
                }
                model = next
                publish(trying = publicModel(next))
                pause()
                return@repeat
            }
            attempts += GeminiRecoveryAttempt(publicModel(model), GeminiRecoveryFault(GeminiRecoveryOutcome.OK), elapsed())
            publish(complete = true)
            return result
        }
        error("Tentativas do Gemini encerradas.")
    }
}
