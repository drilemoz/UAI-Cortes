package br.com.uaicortes

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext

internal data class GeminiBatchStatus(val name: String, val state: String,
    val results: List<Pair<String, String>>, val message: String,
    val issues: Set<String> = emptySet())
internal data class GeminiBatchTextRequest(
    val label: String, val prompt: String, val maxOutputTokens: Int? = null
)

/** API Batch oficial: somente prompts TEXTOS submetidos explicitamente; sem failover/retry de POST. */
internal class GeminiBatchClient {
    private val connection = AtomicReference<HttpURLConnection?>(null)
    fun cancel() { connection.getAndSet(null)?.disconnect() }

    companion object { const val MAX_FLOW_PROMPT_LENGTH = 4400 }

    suspend fun create(key: String, model: String, prompts: List<String>): String =
        createRequests(key, model, prompts.mapIndexed { i, prompt ->
            GeminiBatchTextRequest("texto-${i + 1}", prompt)
        })

    suspend fun createRequests(key: String, model: String,
        requests: List<GeminiBatchTextRequest>): String = withContext(Dispatchers.IO) {
        val id = GeminiBatchPolicy.model(model)
        require(requests.size in 2..GeminiBatchPolicy.MAX_ITEMS)
        require(requests.map { it.label }.distinct().size == requests.size)
        require(requests.all {
            it.label.matches(Regex("[a-z0-9-]{1,40}")) && it.prompt.isNotBlank() &&
                it.prompt.length <= MAX_FLOW_PROMPT_LENGTH &&
                (it.maxOutputTokens == null || it.maxOutputTokens in 1..2048)
        })
        val items = JSONArray()
        requests.forEach { item ->
            val request = JSONObject().put("contents", JSONArray().put(JSONObject()
                .put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", item.prompt)))))
            if (item.maxOutputTokens != null) request.put("generationConfig",
                JSONObject().put("maxOutputTokens", item.maxOutputTokens))
            items.put(JSONObject().put("request", request)
                .put("metadata", JSONObject().put("key", item.label)))
        }
        val body = JSONObject().put("batch", JSONObject()
            .put("display_name", "Uai Cortes - texto")
            .put("input_config", JSONObject().put("requests", JSONObject().put("requests", items))))
        val response = call("https://generativelanguage.googleapis.com/v1beta/models/$id:batchGenerateContent",
            GeminiKeyParser.validate(key), body)
        GeminiBatchPolicy.jobName(response.optString("name"))
    }

    /** Consulta a lista sob ação explícita; não cria nem repete jobs. */
    suspend fun listJobs(key: String): List<String> = withContext(Dispatchers.IO) {
        val json = call("https://generativelanguage.googleapis.com/v1beta/batches?pageSize=50",
            GeminiKeyParser.validate(key), null)
        val array = json.optJSONArray("batches") ?: JSONArray()
        (0 until minOf(array.length(), 50)).mapNotNull { index ->
            val name = array.optJSONObject(index)?.optString("name").orEmpty()
            runCatching { GeminiBatchPolicy.jobName(name) }.getOrNull()
        }.distinct()
    }

    suspend fun status(key: String, name: String): GeminiBatchStatus = withContext(Dispatchers.IO) {
        val valid = GeminiBatchPolicy.jobName(name)
        val obj = call("https://generativelanguage.googleapis.com/v1beta/$valid",
            GeminiKeyParser.validate(key), null)
        val state = obj.optJSONObject("metadata")?.optString("state").orEmpty()
            .ifBlank { obj.optString("state") }
        // REST: output.inlinedResponses.inlinedResponses[]; não confundir o envelope com JSONArray.
        val output = obj.optJSONObject("output") ?: obj.optJSONObject("dest")
            ?: obj.optJSONObject("response")
        val inline = output?.optJSONObject("inlinedResponses")
        val rows = inline?.optJSONArray("inlinedResponses")
            ?: output?.optJSONArray("inlinedResponses") ?: JSONArray()
        val issues = mutableSetOf<String>()
        val results = (0 until minOf(rows.length(), 8)).map { i ->
            val entry = rows.optJSONObject(i) ?: JSONObject()
            val label = entry.optJSONObject("metadata")?.optString("key")?.take(40)
                ?.ifBlank { "Texto ${i + 1}" } ?: "Texto ${i + 1}"
            val content = entry.optJSONObject("response")?.optJSONArray("candidates")
                ?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            val text = if (content == null) "" else (0 until content.length()).joinToString("\n") {
                content.optJSONObject(it)?.optString("text").orEmpty()
            }.trim().take(9000)
            val error = entry.optJSONObject("error")?.optString("message")?.take(160)
            if (text.isBlank() || error != null) issues.add(label)
            label to (text.ifBlank { error ?: "Nenhum texto retornado para esta solicitação." })
        }
        val file = output?.optString("responsesFile").orEmpty().ifBlank {
            output?.optString("fileName").orEmpty()
        }
        GeminiBatchStatus(valid, state, results, when {
            file.isNotBlank() -> "O job retornou um arquivo externo; esta tela aceita somente respostas inline."
            state == "JOB_STATE_SUCCEEDED" && rows.length() == 0 ->
                "Concluído, mas sem respostas inline. Confira os detalhes no AI Studio."
            else -> ""
        }, issues)
    }

    private suspend fun call(address: String, key: String, body: JSONObject?): JSONObject {
        val http = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = if (body == null) "GET" else "POST"
            connectTimeout = 15000
            readTimeout = 60000
            instanceFollowRedirects = false
            useCaches = false
            doOutput = body != null
            setRequestProperty("Accept", "application/json")
            setRequestProperty("x-goog-api-key", key)
            if (body != null) setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        connection.set(http)
        try {
            coroutineContext.ensureActive()
            if (body != null) http.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = http.responseCode
            DiagnosticRecorder.api("gemini", if (body == null) "batch_poll" else "batch_create",
                null, status, null, outcome = if (status in 200..299) "ok" else "http_error")
            coroutineContext.ensureActive()
            if (status !in 200..299) throw IOException(when (status) {
                400 -> "Lote ou modelo rejeitado pelo Gemini (400)."
                401, 403 -> "A chave não possui autorização para este job (HTTP $status)."
                402 -> "Batch exige faturamento habilitado para o projeto (402)."
                404 -> "Modelo, job ou recurso Batch indisponível (404)."
                429 -> "Limite da API Batch atingido (429)."
                else -> "Falha na API Batch (HTTP $status)."
            })
            val reader = http.inputStream.bufferedReader()
            val text = reader.use { input ->
                val out = StringBuilder()
                val buffer = CharArray(2048)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    if (out.length + n > 300_000) throw IOException("Resposta de lote grande demais para esta tela.")
                    out.append(buffer, 0, n)
                }
                out.toString()
            }
            coroutineContext.ensureActive()
            return JSONObject(text)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: java.io.IOException) {
            DiagnosticRecorder.api("gemini", if (body == null) "batch_poll" else "batch_create",
                null, null, null, outcome = "network_uncertain")
            throw error
        } finally {
            connection.compareAndSet(http, null)
            http.disconnect()
        }
    }
}
