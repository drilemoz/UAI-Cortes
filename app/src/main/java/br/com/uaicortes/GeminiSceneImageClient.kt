package br.com.uaicortes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext

internal data class StudioImageModel(val id: String, val label: String)

internal object StudioImagePricing {
    /** Referências oficiais em USD: somente saída de UMA imagem 1K, sem tokens de entrada, texto e impostos. */
    fun output1kUsd(model: String): Double? = when (model) {
        "gemini-3.1-flash-lite-image" -> 0.0336
        "gemini-3.1-flash-image" -> 0.067
        "gemini-3-pro-image" -> 0.134
        else -> null
    }
    fun note(model: String): String = output1kUsd(model)?.let { rate ->
        "Referência pública em 05/11/2026: ~US$ ${"%.4f".format(java.util.Locale.US, rate)} " +
            "por saída 1K. Entrada, texto, tributos e cobrança efetiva não incluídos. " +
            "Confirme as tarifas atuais no Google AI Studio."
    } ?: "Custo não calculável com segurança para este modelo. Consulte a cobrança oficial antes de gerar."
}

/** Endpoint de IMAGEM Gemini apenas; nunca usa endpoints Veo ou pagamento automático. */
internal class GeminiSceneImageClient(context: Context) {
    private val ledger = GeminiUsageLedger(context)
    private val running = AtomicReference<HttpURLConnection?>(null)
    fun cancel() { running.getAndSet(null)?.disconnect() }

    /** models.list verifica a chave sem disparar geração. */
    suspend fun listImageModels(key: String): List<StudioImageModel> = withContext(Dispatchers.IO) {
        val safe = GeminiKeyParser.validate(key)
        val models = mutableListOf<StudioImageModel>()
        var next: String? = null
        var pages = 0
        do {
            val address = "https://generativelanguage.googleapis.com/v1beta/models?pageSize=100" +
                (next?.let { "&pageToken=" + URLEncoder.encode(it, "UTF-8") } ?: "")
            val data = fetch(URL(address), safe, null, 300_000)
            val rows = data.optJSONArray("models") ?: JSONArray()
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val id = row.optString("name").removePrefix("models/")
                val methods = row.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                val supported = (0 until methods.length()).any { methods.optString(it) == "generateContent" }
                if (supported && id.matches(Regex("gemini-[a-z0-9._-]{3,80}-image"))) {
                    models += StudioImageModel(id, row.optString("displayName").take(70).ifBlank { id })
                }
            }
            next = data.optString("nextPageToken").takeIf { it.isNotBlank() }
            pages++
        } while (next != null && pages < 5)
        models.distinctBy { it.id }.sortedBy { it.id }.also {
            if (it.isEmpty()) throw IOException("Nenhum modelo de imagem disponível na listagem desta chave.")
        }
    }

    suspend fun createImage(key: String, model: String, description: String,
        reference: List<ByteArray>): ByteArray = withContext(Dispatchers.IO) {
        require(model.matches(Regex("gemini-[a-z0-9._-]{3,80}-image"))) { "Modelo de imagem inválido." }
        require(description.length in 10..20000) { "Descrição da imagem inválida." }
        require(reference.size <= 2) { "Até duas referências por geração." }
        val parts = JSONArray().put(JSONObject().put("text", description))
        reference.forEach { bytes ->
            val compressed = compressReference(bytes)
            parts.put(JSONObject().put("inlineData", JSONObject()
                .put("mimeType", "image/jpeg")
                .put("data", Base64.encodeToString(compressed, Base64.NO_WRAP))))
        }
        val request = JSONObject().put("contents", JSONArray().put(JSONObject()
            .put("role", "user").put("parts", parts)))
            .put("generationConfig", JSONObject()
                .put("responseModalities", JSONArray().put("IMAGE").put("TEXT"))
                .put("imageConfig", JSONObject().put("aspectRatio", "9:16")))
        val started = System.nanoTime()
        val safe = GeminiKeyParser.validate(key)
        var actualResponse: JSONObject? = null
        var recorded = false
        try {
            val result = fetch(URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"),
                safe, request, 24_000_000)
            actualResponse = result
            val blocks = result.optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts")
                ?: throw IOException("Gemini não retornou imagem. Verifique o modelo e as restrições.")
            for (i in 0 until blocks.length()) {
                val part = blocks.optJSONObject(i) ?: continue
                val data = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data") ?: continue
                val mime = data.optString("mimeType", data.optString("mime_type"))
                if (mime != "image/png" && mime != "image/jpeg" && mime != "image/webp") continue
                val encoded = data.optString("data")
                if (encoded.length > 16_000_000) throw IOException("Imagem maior que o limite do Estúdio.")
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                if (bytes.size in 100..12_000_000) {
                    recorded = true
                    ledger.safeRecord(safe, model, "studio_image", result,
                        latencyMs = (System.nanoTime() - started) / 1_000_000)
                    return@withContext bytes
                }
            }
            throw IOException("Nenhuma imagem utilizável foi retornada pelo Gemini.")
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) {
            if (!recorded) ledger.safeRecord(safe, model, "studio_image", actualResponse,
                status = if (failure is GeminiHttpException) "erro_${failure.statusCode}" else "erro",
                latencyMs = (System.nanoTime() - started) / 1_000_000)
            throw failure
        }
    }

    private fun compressReference(input: ByteArray): ByteArray {
        require(input.size <= 12_000_000)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(input, 0, input.size, bounds)
        require(bounds.outWidth in 1..10000 && bounds.outHeight in 1..15000)
        var scale = 1
        while (bounds.outWidth / scale > 900 || bounds.outHeight / scale > 1400) scale *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = scale }
        val bitmap = BitmapFactory.decodeByteArray(input, 0, input.size, opts)
            ?: throw IOException("Não foi possível decodificar a referência.")
        return try {
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 72, out)
                out.toByteArray().also { require(it.size <= 2_000_000) }
            }
        } finally { bitmap.recycle() }
    }

    private suspend fun fetch(url: URL, key: String, body: JSONObject?, maxBytes: Int): JSONObject {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 90000
            instanceFollowRedirects = false
            requestMethod = if (body == null) "GET" else "POST"
            doOutput = body != null
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("x-goog-api-key", key)
            if (body != null) setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        running.set(connection)
        try {
            coroutineContext.ensureActive()
            body?.let { connection.outputStream.use { output -> output.write(it.toString().toByteArray()) } }
            val code = connection.responseCode
            coroutineContext.ensureActive()
            if (code !in 200..299) throw GeminiHttpException(code, when (code) {
                400 -> "A solicitação de imagem foi rejeitada pelo Gemini (400)."
                401, 403 -> "Chave sem autorização para este modelo de imagem (HTTP $code)."
                402 -> "O projeto exige configuração de pagamento (402)."
                404 -> "Modelo de imagem indisponível para a chave (404)."
                429 -> "Cota do Gemini alcançada (429)."
                else -> "Não foi possível gerar a imagem (HTTP $code). Resultado pode ser incerto."
            })
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buf = ByteArray(8192)
                while (true) {
                    coroutineContext.ensureActive()
                    val n = input.read(buf)
                    if (n < 0) break
                    if (output.size() + n > maxBytes) throw IOException("Resposta de imagem maior que o limite permitido.")
                    output.write(buf, 0, n)
                }
            }
            return JSONObject(output.toString("UTF-8"))
        } catch (cancel: CancellationException) { throw cancel }
        catch (err: java.net.UnknownHostException) { throw IOException("Sem internet para acessar o Gemini.") }
        catch (err: java.net.SocketTimeoutException) { throw IOException("Tempo esgotado. Confira o histórico antes de tentar novamente; a chamada pode ter sido cobrada.") }
        finally { running.compareAndSet(connection, null); connection.disconnect() }
    }
}
