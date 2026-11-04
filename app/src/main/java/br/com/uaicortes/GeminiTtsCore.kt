package br.com.uaicortes

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext

/** Parser puro: nunca incluir credenciais nas mensagens de erro, telemetria ou diagnósticos. */
internal object GeminiKeyParser {
    fun parse(content: String): String {
        require(content.toByteArray(Charsets.UTF_8).size <= 64 * 1024) { "Arquivo de chave muito grande." }
        val trimmed = content.trim().removePrefix("\uFEFF")
        val candidate = if (trimmed.startsWith("{")) {
            val obj = JSONObject(trimmed)
            val selected = obj.opt("GEMINI_API_KEY") ?: obj.opt("GOOGLE_API_KEY")
            if (selected is String) selected else ""
        } else {
            val lines = trimmed.lineSequence().map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }.toList()
            val named = lines.firstOrNull { it.matches(Regex("(?:export\\s+)?(?:GEMINI_API_KEY|GOOGLE_API_KEY)\\s*=.*")) }
            if (named != null) named.substringAfter('=').trim().trim('"', '\'')
            else if (lines.size == 1 && !lines[0].contains('=')) lines[0].trim('"', '\'')
            else ""
        }
        return validate(candidate)
    }
    fun validate(key: String): String {
        val value = key.trim()
        require(value.length in 12..512 && value.none { it.isWhitespace() || it.isISOControl() } &&
            value.all { it.code in 33..126 }) { "Chave ausente ou conteúdo inválido." }
        return value
    }
    fun masked(key: String): String = if (key.isEmpty()) "(não configurada)"
        else "••••••••${key.takeLast(4)}"
}

internal object GeminiModels {
    const val FLASH = "gemini-3.8-flash-tts"
    const val LITE = "gemini-3.8-flash-lite-tts"
    val supported = listOf(LITE, FLASH)
    fun requireSupported(model: String): String = model.also {
        require(it in supported) { "Modelo TTS não reconhecido." }
    }
}

internal data class GeminiVoice(
    val id: String, val name: String, val description: String,
    val languageCode: String = "", val accent: String = "", val persona: String = "",
    val context: String = "", val gender: String = "", val pitch: String = ""
)
internal data class GeminiWave(val bytes: ByteArray, val durationMs: Long)

/** Valida RIFF/WAVE PCM mono 16-bit; não cria outro cabeçalho. */
internal object GeminiWaveParser {
    private fun u16(b: ByteArray, pos: Int): Int = (b[pos].toInt() and 255) or ((b[pos + 1].toInt() and 255) shl 8)
    private fun u32(b: ByteArray, pos: Int): Long =
        (b[pos].toLong() and 255) or ((b[pos + 1].toLong() and 255) shl 8) or
            ((b[pos + 2].toLong() and 255) shl 16) or ((b[pos + 3].toLong() and 255) shl 24)
    fun duration(bytes: ByteArray, mime: String): Long {
        require(mime.substringBefore(';').lowercase() in setOf("audio/wav", "audio/x-wav", "audio/wave")) {
            "O serviço não devolveu áudio WAV."
        }
        require(bytes.size in 45..(32 * 1024 * 1024)) { "Áudio vazio ou acima do limite de segurança." }
        fun chunkName(p: Int) = String(bytes, p, 4, Charsets.US_ASCII)
        require(chunkName(0) == "RIFF" && chunkName(8) == "WAVE") { "Cabeçalho WAV inválido." }
        require(u32(bytes, 4) + 8 == bytes.size.toLong()) { "Tamanho RIFF inconsistente." }
        var at = 12
        var bytesPerSecond = 0L
        var dataSize = -1L
        while (at + 8 <= bytes.size) {
            val id = chunkName(at)
            val size = u32(bytes, at + 4)
            val start = at.toLong() + 8
            require(size <= bytes.size.toLong() - start) { "Bloco WAV incompleto." }
            if (id == "fmt ") {
                require(size >= 16) { "Formato WAV incompleto." }
                val fmt = u16(bytes, at + 8)
                val channels = u16(bytes, at + 10)
                val rate = u32(bytes, at + 12)
                val byteRate = u32(bytes, at + 16)
                val bits = u16(bytes, at + 22)
                require(fmt == 1 && channels == 1 && bits == 16 && rate in 8000L..96000L &&
                    byteRate == rate * 2) { "Formato PCM não suportado." }
                bytesPerSecond = byteRate
            }
            if (id == "data") dataSize = size
            at = (start + size + (size and 1L)).toInt()
        }
        require(bytesPerSecond > 0 && dataSize > 0 && dataSize % 2L == 0L) { "WAV sem dados PCM válidos." }
        val duration = dataSize * 1000L / bytesPerSecond
        require(duration >= 200L) { "Áudio curto ou inválido." }
        return duration
    }
}

/** Rede: uma única solicitação por ação, sem retry automático; nenhum texto ou chave em logs. */
internal class GeminiTtsClient(context: Context? = null) {
    private val failover = context?.let { GeminiKeyFailover(it) }
    private val active = AtomicReference<HttpURLConnection?>(null)
    fun cancel() { active.getAndSet(null)?.disconnect() }
    private suspend fun request(path: String, key: String, body: JSONObject? = null, max: Int = 1024 * 1024): JSONObject =
        if (failover == null) requestOnce(path, key, body, max)
        else failover.run(key) { credential -> requestOnce(path, credential, body, max) }

    private suspend fun requestOnce(path: String, key: String, body: JSONObject? = null, max: Int = 1024 * 1024): JSONObject =
        withContext(Dispatchers.IO) {
            coroutineContext.ensureActive()
            val conn = URL("https://generativelanguage.googleapis.com/v1beta/$path").openConnection() as HttpURLConnection
            active.set(conn)
            try {
                conn.requestMethod = if (body == null) "GET" else "POST"
                conn.setRequestProperty("x-goog-api-key", key)
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 12000
                conn.readTimeout = 120000
                conn.useCaches = false
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val status = conn.responseCode
                coroutineContext.ensureActive()
                if (status !in 200..299) {
                    val safeCode = runCatching {
                        val error = conn.errorStream?.use { stream -> readLimit(stream, 64 * 1024) }
                        JSONObject(error?.toString(Charsets.UTF_8) ?: "{}").optJSONObject("error")?.let { e ->
                            val codes = mutableListOf(e.optString("status"))
                            val details = e.optJSONArray("details")
                            if (details != null) for (i in 0 until details.length()) {
                                details.optJSONObject(i)?.optString("reason")?.let { codes += it }
                            }
                            codes.joinToString(" ")
                        }
                    }.getOrNull().orEmpty()
                    throw GeminiApiError(status, safeCode)
                }
                val result = conn.inputStream.use { readLimit(it, max) }
                coroutineContext.ensureActive()
                JSONObject(result.toString(Charsets.UTF_8))
            } catch (e: UnknownHostException) { throw IOException("Sem conexão com a internet.") }
            catch (e: SocketTimeoutException) { throw IOException("A conexão com Gemini excedeu o tempo limite.") }
            finally { active.compareAndSet(conn, null); conn.disconnect() }
        }
    private fun readLimit(input: java.io.InputStream, max: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            if (output.size() + n > max) throw IOException("Resposta maior que o limite permitido.")
            output.write(buf, 0, n)
        }
        return output.toByteArray()
    }
    suspend fun checkModel(key: String, model: String): Boolean {
        GeminiModels.requireSupported(model)
        return try {
            request("models/$model", key)
            true
        } catch (e: GeminiApiError) {
            if (e.http == 404) false else throw e
        }
    }
    /** A consulta não sintetiza áudio nem envia texto/legendas. */
    suspend fun voicesWithDiagnosis(key: String): GeminiVoiceCatalogResult =
        GeminiVoiceCatalog.fetchCatalog { path -> request(path, key) }

    suspend fun voices(key: String): List<GeminiVoice> = voicesWithDiagnosis(key).voices
    suspend fun synthesize(key: String, model: String, voice: String, style: String, text: String): GeminiWave {
        GeminiModels.requireSupported(model)
        require(voice.isNotBlank() && voice.length <= 150) { "Selecione uma voz disponível." }
        require(text.isNotBlank() && text.length <= 4000) { "Digite entre 1 e 4.000 caracteres." }
        val content = JSONObject().put("type", "text").put("text", text)
            .put("annotations", JSONArray().put(JSONObject().put("type", "speech_metadata").put("style", style)))
        val body = JSONObject().put("model", model)
            .put("input", JSONArray().put(JSONObject().put("type", "user_input")
                .put("content", JSONArray().put(content))))
            .put("response_format", JSONObject().put("type", "audio").put("mime_type", "audio/wav"))
            .put("generation_config", JSONObject().put("speech_config", JSONArray().put(JSONObject().put("voice", voice))))
        val result = request("interactions", key, body, 44 * 1024 * 1024)
        val steps = result.optJSONArray("steps") ?: throw IOException("Resposta sem etapas de áudio.")
        var encoded: String? = null
        var mime = ""
        for (i in 0 until steps.length()) {
            val step = steps.optJSONObject(i) ?: continue
            if (step.optString("type") != "model_output") continue
            val parts = step.optJSONArray("content") ?: continue
            for (j in 0 until parts.length()) {
                val part = parts.optJSONObject(j) ?: continue
                if (part.optString("type") == "audio") {
                    encoded = part.optString("data")
                    mime = part.optString("mime_type", part.optString("mimeType", "audio/wav"))
                }
            }
        }
        if (encoded.isNullOrBlank()) throw IOException("Gemini não retornou áudio.")
        val bytes = try { Base64.decode(encoded, Base64.DEFAULT) }
            catch (_: IllegalArgumentException) { throw IOException("Áudio Base64 inválido.") }
        return GeminiWave(bytes, GeminiWaveParser.duration(bytes, mime.ifBlank { "audio/wav" }))
    }
}

internal class GeminiApiError(val http: Int, private val status: String): IOException(messageFor(http, status)) {
    companion object {
        internal fun messageFor(http: Int, status: String): String = when {
            http == 401 || status.contains("UNAUTHENTICATED") || status.contains("API_KEY_INVALID") || status.contains("API_KEY_EXPIRED") -> "Chave inválida ou revogada (401)."
            http == 403 && (status.contains("BILLING") || status.contains("PAYMENT")) -> "Faturamento não habilitado ou bloqueado (403)."
            http == 403 -> "Chave sem autorização para o serviço (403). Verifique também faturamento e restrições da chave."
            http == 404 -> "Modelo ou recurso indisponível para este projeto (404)."
            http == 429 || status == "RESOURCE_EXHAUSTED" -> "Cota ou limite de utilização atingido (429). Confira o faturamento."
            http == 402 -> "Problema de faturamento (402)."
            status.contains("BILLING") || status.contains("PAYMENT") -> "Verifique faturamento e cota do projeto Gemini." 
            status.contains("FAILED_PRECONDITION") -> "Pré-condição não atendida. Verifique faturamento, cota e acesso ao modelo."
            http >= 500 -> "Falha temporária do servidor Gemini ($http)."
            else -> "Gemini recusou a solicitação (HTTP $http, $status)."
        }
    }
}

/** Guarda mídia em armazenamento interno durável, nunca em cache; projetos referenciam file:// local. */
internal object GeminiAudioFiles {
    fun save(context: Context, wave: GeminiWave): File {
        val dir = File(context.filesDir, "gemini_narrations")
        if (!dir.exists() && !dir.mkdirs()) throw IOException("Falha ao criar diretório de áudios.")
        val reserve = wave.bytes.size.toLong() + 4L * 1024 * 1024
        if (StatFs(dir.path).availableBytes < reserve) throw IOException("Espaço insuficiente para salvar a narração.")
        val name = "narracao_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.wav"
        val target = File(dir, name)
        val temp = File(dir, "$name.part")
        try {
            temp.outputStream().use { out -> out.write(wave.bytes); out.fd.sync() }
            if (!temp.renameTo(target)) throw IOException("Não foi possível concluir a gravação.")
            return target
        } finally { temp.delete() }
    }
    fun uri(file: File): String = Uri.fromFile(file).toString()
}
