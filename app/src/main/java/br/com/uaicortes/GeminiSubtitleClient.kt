package br.com.uaicortes

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

/** Gemini ASR opcional: só envia áudio após confirmação visível. Não reutiliza o TTS. */
internal class GeminiSubtitleClient(context: Context? = null) {
    private val usage = context?.let { GeminiUsageLedger(it) }
    private val active = AtomicReference<HttpURLConnection?>(null)
    fun cancel() { active.getAndSet(null)?.disconnect() }

    private fun connection(url: String, key: String, method: String): HttpURLConnection {
        val checked = URL(url)
        require(checked.protocol == "https" &&
            (checked.host == "generativelanguage.googleapis.com" || checked.host.endsWith(".googleapis.com"))) {
            "Servidor de upload não autorizado."
        }
        return (checked.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 150000
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("x-goog-api-key", key)
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun limited(input: InputStream?, max: Int = 2 * 1024 * 1024): ByteArray {
        if (input == null) return ByteArray(0)
        return input.use { stream ->
            val result = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                if (result.size() + count > max) throw IOException("Resposta Gemini acima do limite permitido.")
                result.write(buffer, 0, count)
            }
            result.toByteArray()
        }
    }

    private fun checkedResult(conn: HttpURLConnection): JSONObject {
        val code = conn.responseCode
        if (code !in 200..299) {
            // Não incluir resposta bruta: ela pode conter trechos privados do usuário.
            val name = when (code) {
                400 -> "Requisição de transcrição inválida."
                401, 403 -> "API sem autorização para o modelo de transcrição."
                404 -> "Recurso Gemini não encontrado."
                429 -> "Cota ou limite de solicitações da API atingido."
                in 500..599 -> "Servidor Gemini indisponível."
                else -> "Servidor retornou HTTP $code."
            }
            throw GeminiHttpException(code, name)
        }
        return JSONObject(limited(conn.inputStream).toString(Charsets.UTF_8))
    }

    private suspend fun upload(file: File, key: String,
        onProgress: suspend (Int?, String) -> Unit,
        onPhase: (SubtitleCloudPhase) -> Unit): Triple<String,String,String> {
        val start = connection("https://generativelanguage.googleapis.com/upload/v1beta/files",key,"POST")
        active.set(start)
        val uploadUrl: String
        try {
            start.doOutput = true
            start.setRequestProperty("X-Goog-Upload-Protocol", "resumable")
            start.setRequestProperty("X-Goog-Upload-Command", "start")
            start.setRequestProperty("X-Goog-Upload-Header-Content-Type", "audio/wav")
            start.setRequestProperty("X-Goog-Upload-Header-Content-Length", file.length().toString())
            start.setRequestProperty("Content-Type", "application/json")
            start.outputStream.use { it.write("{\"file\":{\"display_name\":\"Uai Cortes audio\"}}".toByteArray()) }
            if(start.responseCode !in 200..299) checkedResult(start)
            uploadUrl = start.getHeaderField("X-Goog-Upload-URL")
                ?: throw IOException("Servidor não retornou endereço de upload.")
        } finally { active.compareAndSet(start,null); start.disconnect() }
        currentCoroutineContext().ensureActive()
        onPhase(SubtitleCloudPhase.UPLOAD_BYTES)
        val conn = connection(uploadUrl,key,"POST")
        active.set(conn)
        try {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type","audio/wav")
            conn.setRequestProperty("X-Goog-Upload-Offset","0")
            conn.setRequestProperty("X-Goog-Upload-Command","upload, finalize")
            conn.setFixedLengthStreamingMode(file.length())
            conn.outputStream.use { output ->
                file.inputStream().use { input ->
                    val buffer = ByteArray(32 * 1024)
                    var sent = 0L
                    var lastPercent = -1
                    while(true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if(n < 0) break
                        output.write(buffer,0,n)
                        sent += n
                        val percent=(60 + sent*30/file.length().coerceAtLeast(1L)).toInt()
                        if(percent != lastPercent) {
                            onProgress(percent,"Enviando áudio à API Gemini…")
                            lastPercent=percent
                        }
                    }
                }
            }
            val uploaded = checkedResult(conn).optJSONObject("file")
                ?: throw IOException("Gemini não confirmou o arquivo de áudio.")
            val uri=uploaded.optString("uri")
            val name=uploaded.optString("name")
            require(uri.startsWith("https://") && name.matches(Regex("files/[A-Za-z0-9_-]+"))) {
                "Resposta de upload inválida."
            }
            return Triple(uri,name,uploaded.optString("state"))
        } finally { active.compareAndSet(conn,null); conn.disconnect() }
    }

    private fun deleteUploaded(name: String, key: String) {
        if (!name.matches(Regex("files/[A-Za-z0-9_-]+"))) return
        val conn = connection("https://generativelanguage.googleapis.com/v1beta/$name",key,"DELETE")
        active.set(conn)
        conn.connectTimeout=5000
        conn.readTimeout=5000
        try { conn.responseCode } finally { active.compareAndSet(conn,null);conn.disconnect() }
    }

    private suspend fun waitForFile(name: String, key: String) {
        val deadline = System.nanoTime() + 60_000_000_000L
        while (System.nanoTime() < deadline) {
            currentCoroutineContext().ensureActive()
            val conn=connection("https://generativelanguage.googleapis.com/v1beta/$name",key,"GET")
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            active.set(conn)
            val response=try { checkedResult(conn) }
                finally {active.compareAndSet(conn,null);conn.disconnect()}
            val state = response.optString("state").ifBlank {
                response.optJSONObject("file")?.optString("state").orEmpty()
            }
            if(state == "ACTIVE") return
            if(state == "FAILED" || state.isBlank()) throw SubtitleCloudException(
                SubtitleCloudIssue(SubtitleCloudPhase.PROCESS_FILE, SubtitleCloudReason.FILE_PROCESSING))
            delay(2000L)
        }
        throw SubtitleCloudException(
            SubtitleCloudIssue(SubtitleCloudPhase.PROCESS_FILE, SubtitleCloudReason.TIMEOUT))
    }

    private fun post(endpoint: String, body: JSONObject, key: String): JSONObject {
        val conn = connection("https://generativelanguage.googleapis.com/v1beta/$endpoint",key,"POST")
        active.set(conn)
        try {
            conn.doOutput=true
            conn.setRequestProperty("Content-Type","application/json; charset=utf-8")
            val bytes=body.toString().toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(bytes.size)
            conn.outputStream.use { it.write(bytes) }
            return checkedResult(conn)
        } finally { active.compareAndSet(conn,null);conn.disconnect() }
    }

    /** O teste usa a chave ativa e áudio embutido, sem projetos nem Files API. */
    suspend fun probe(key: String, openSample: () -> InputStream): GeminiSubtitleProbe = withContext(Dispatchers.IO) {
        val valid = GeminiKeyParser.validate(key)
        val input = GeminiTranscriptionRequest.inline(limited(openSample(), GeminiTranscriptionRequest.PROBE_MAX_BYTES))
        GeminiModelRecovery.execute(GeminiRecoveryRoute.TRANSCRIBE, GeminiTranscriptionRequest.MODEL) { model, _ ->
            val cues = attempt(model, input, valid, GeminiTranscriptionRequest.PROBE_DURATION_MS, false, sample = true)
            val count = if (model == GeminiLiteTranscription.MODEL) cues.size else cues.sumOf { it.wordTimings.size }
            if (count == 0) throw SubtitleCloudException(SubtitleCloudIssue(
                SubtitleCloudPhase.READ_RESULT, SubtitleCloudReason.INVALID_RESULT))
            GeminiSubtitleProbe(model, count)
        }
    }

    private suspend fun attempt(model: String, input: JSONObject, valid: String, durationMs: Long,
        withSpeakerLabels: Boolean, sample: Boolean = false): List<SubtitleCue> {
        val lite = model == GeminiLiteTranscription.MODEL
        check(lite || model == GeminiTranscriptionRequest.MODEL)
        val started = System.nanoTime()
        fun elapsed() = ((System.nanoTime() - started) / 1_000_000L).coerceAtLeast(0)
        val body = if (lite) GeminiLiteTranscription.body(input, durationMs, sample)
            else GeminiTranscriptionRequest.body(input, withSpeakerLabels, sample)
        val response = try {
            if (lite) post("models/$model:generateContent", body, valid)
            else post("interactions", body, valid)
        } catch (failure: GeminiHttpException) {
            usage?.safeRecord(valid, model, "legendas",
                status = if (failure.statusCode == 429) "429" else "erro_${failure.statusCode}",
                latencyMs = elapsed())
            throw SubtitleCloudException(SubtitleCloudIssue.from(SubtitleCloudPhase.TRANSCRIBE, failure))
        }
        usage?.safeRecord(valid, model, "legendas", response, latencyMs = elapsed())
        currentCoroutineContext().ensureActive()
        try {
            if (lite) return GeminiLiteTranscription.parse(response, durationMs)
            val state = response.optString("status")
            require(state.isBlank() || state == "completed")
            return GeminiSubtitleParser.parse(response, durationMs, withSpeakerLabels).also { require(it.isNotEmpty()) }
        } catch (_: Exception) {
            throw SubtitleCloudException(SubtitleCloudIssue(
                SubtitleCloudPhase.READ_RESULT, SubtitleCloudReason.INVALID_RESULT))
        }
    }

    suspend fun transcribe(context: Context, project: VideoProject, key: String,
        withSpeakerLabels: Boolean,
        onProgress: suspend (Int?,String) -> Unit): List<SubtitleCue> = withContext(Dispatchers.IO) {
        var phase = SubtitleCloudPhase.KEY
        var valid = ""
        var remote: String? = null
        try {
            var usedModel = GeminiTranscriptionRequest.MODEL
            var cachedAudio: File? = null
            var preparedInput: JSONObject? = null
            // Uma fonte e uma chave para as duas tentativas, inclusive se o WAV for grande.
            val parsed = GeminiModelRecovery.execute(GeminiRecoveryRoute.TRANSCRIBE, usedModel) { model, _ ->
                usedModel = model
                val input = preparedInput ?: try {
                    valid = GeminiKeyParser.validate(key)
                    phase = SubtitleCloudPhase.AUDIO
                    val wav = SubtitleAudioCache.getOrPrepare(context, project, onProgress)
                    cachedAudio = wav
                    val prepared = if (GeminiTranscriptionRequest.useInline(wav.length())) {
                        GeminiTranscriptionRequest.inline(limited(wav.inputStream(), GeminiTranscriptionRequest.INLINE_MAX_BYTES))
                    } else {
                        phase = SubtitleCloudPhase.UPLOAD_START
                        onProgress(60,"Preparando envio do áudio ao Gemini…")
                        val (uri,name,state) = upload(wav, valid, onProgress) { phase = it }
                        remote = name
                        if (state != "ACTIVE") {
                            phase = SubtitleCloudPhase.PROCESS_FILE
                            onProgress(null,"Aguardando o processamento do áudio no Gemini…")
                            waitForFile(name, valid)
                        }
                        GeminiTranscriptionRequest.file(uri)
                    }
                    prepared.also { preparedInput = it }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (error: Exception) {
                    throw SubtitleCloudException(SubtitleCloudIssue.from(phase, error))
                }
                currentCoroutineContext().ensureActive()
                phase = SubtitleCloudPhase.TRANSCRIBE
                onProgress(null, if (model == GeminiLiteTranscription.MODEL)
                    "Tentando Gemini 3.5 Flash-Lite…" else "Transcrevendo fala e identificando horários…")
                attempt(model, input, valid, project.durationMs, withSpeakerLabels)
            }
            if (usedModel == GeminiLiteTranscription.MODEL) {
                onProgress(100, GeminiLiteTranscription.NOTICE)
                return@withContext parsed
            }
            // Confere os horários do Gemini com a energia real do áudio enviado.
            val refined=runCatching {
                SubtitleTimingRefiner.refine(parsed,SubtitleVoiceEnvelope.read(checkNotNull(cachedAudio)),project.durationMs)
            }.getOrNull()
            val cues=refined?.cues ?: parsed
            val note=if (refined != null && (refined.globalShiftMs != 0L || refined.snappedCount > 0 || refined.wordsSnapped > 0))
                "Transcrição concluída · horários conferidos com o áudio (${refined.snappedCount} legendas, ${refined.wordsSnapped} palavras)"
            else "Transcrição concluída"
            onProgress(100,note)
            cues
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            val issue = SubtitleCloudIssue.from(phase, failure)
            DiagnosticRecorder.subtitleCloudFailure("transcribe", issue)
            throw SubtitleCloudException(issue)
        }
        finally {
            // O WAV permanece em cache privado por até 3 horas, sujeito à limpeza.
            remote?.let { runCatching { deleteUploaded(it,valid) } }
        }
    }
}
