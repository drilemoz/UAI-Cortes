package br.com.uaicortes

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicReference

/** Gemini ASR opcional: só envia áudio após confirmação visível. Não reutiliza o TTS. */
internal class GeminiSubtitleClient(context: Context? = null) {
    private val failover = context?.let { GeminiKeyFailover(it) }
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
                404 -> "Modelo Gemini Transcribe não disponível para esta chave."
                429 -> "Cota ou limite de solicitações da API atingido."
                in 500..599 -> "Servidor Gemini indisponível."
                else -> "Servidor retornou HTTP $code."
            }
            throw GeminiHttpException(code, name)
        }
        return JSONObject(limited(conn.inputStream).toString(Charsets.UTF_8))
    }

    private suspend fun upload(file: File, key: String,
        onProgress: suspend (Int?, String) -> Unit): Triple<String,String,String> {
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
        repeat(10) {
            currentCoroutineContext().ensureActive()
            val conn=connection("https://generativelanguage.googleapis.com/v1beta/$name",key,"GET")
            active.set(conn)
            val response=try { checkedResult(conn) }
                finally {active.compareAndSet(conn,null);conn.disconnect()}
            val state=response.optJSONObject("file")?.optString("state").orEmpty()
            if(state == "ACTIVE") return
            if(state == "FAILED") throw IOException("O Gemini não conseguiu processar o áudio.")
            delay(1300L)
        }
        throw IOException("O áudio ainda não ficou disponível no Gemini. Tente novamente depois.")
    }

    private fun interaction(uri: String, key: String): JSONObject {
        val body=JSONObject()
            .put("model","gemini-3.5-transcribe")
            .put("input",JSONArray().put(JSONObject().put("type","audio")
                .put("uri",uri).put("mime_type","audio/wav")))
            .put("generation_config", JSONObject().put("transcription_config",
                JSONObject().put("language_codes",JSONArray().put("pt-BR"))
                    .put("mode",JSONObject().put("type","verbatim")
                        .put("timestamp_granularities",JSONArray().put("word")))))
        val conn = connection("https://generativelanguage.googleapis.com/v1beta/interactions",key,"POST")
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

    suspend fun transcribe(context: Context, project: VideoProject, key: String,
        onProgress: suspend (Int?,String) -> Unit): List<SubtitleCue> =
        if (failover == null) transcribeOnce(context, project, key, onProgress)
        else failover.run(key) { credential -> transcribeOnce(context, project, credential, onProgress) }

    private suspend fun transcribeOnce(context: Context, project: VideoProject, key: String,
        onProgress: suspend (Int?,String) -> Unit): List<SubtitleCue> = withContext(Dispatchers.IO) {
        val valid=GeminiKeyParser.validate(key)
        val wav=SubtitleAudioCache.getOrPrepare(context,project,onProgress)
        var remote: String? = null
        try {
            onProgress(60,"Preparando envio do áudio ao Gemini…")
            val (uri,name,state)=upload(wav,valid,onProgress)
            remote=name
            if(state != "ACTIVE") {
                onProgress(null,"Aguardando o processamento do áudio no Gemini…")
                waitForFile(name,valid)
            }
            currentCoroutineContext().ensureActive()
            onProgress(null,"Transcrevendo fala e identificando horários…")
            val response=interaction(uri,valid)
            currentCoroutineContext().ensureActive()
            val cues=GeminiSubtitleParser.parse(response,project.durationMs)
            onProgress(100,"Transcrição concluída")
            cues
        } catch(e: UnknownHostException) {throw IOException("Sem conexão com a internet.")}
        catch(e: SocketTimeoutException) {throw IOException("O Gemini demorou a responder.")}
        finally {
            // O WAV permanece em cache privado por até 3 horas, sujeito à limpeza.
            remote?.let { runCatching { deleteUploaded(it,valid) } }
        }
    }
}
