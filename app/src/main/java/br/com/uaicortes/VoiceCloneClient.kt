package br.com.uaicortes

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.coroutines.coroutineContext

/** Somente HTTPS com certificado confiável. Nenhuma requisição automática/repetida. */
internal object VoiceCloneClient {
    suspend fun synthesize(context: Context, origin: String, token: String,
        profile: VoiceCloneProfile, text: String): File = withContext(Dispatchers.IO) {
        val endpoint = VoiceClonePolicy.checkEndpoint(origin)
        require(token.length in 20..256 && token.none { it.isWhitespace() || it.isISOControl() }) {
            "Informe o token do servidor de voz (20–256 caracteres)."
        }
        require(VoiceClonePolicy.engineAllowed(profile.engine))
        require(text.isNotBlank() && text.length <= 800) {
            "Cada geração aceita até 800 caracteres de texto."
        }
        val reference = VoiceCloneLibrary(context).reference(profile)
        require(reference.isFile && reference.length() in 128..VoiceClonePolicy.MAX_REFERENCE) {
            "Referência de voz não está disponível neste aparelho."
        }
        val encoded = Base64.encodeToString(reference.readBytes(), Base64.NO_WRAP)
        val payload = JSONObject().put("engine", profile.engine)
            .put("voice_id", profile.id).put("text", text).put("language", "pt")
            .put("reference_wav_b64", encoded).toString().toByteArray(Charsets.UTF_8)
        val folder = File(context.filesDir, "voice_clone_outputs_v1")
        if (!folder.exists() && !folder.mkdirs()) throw IOException("Sem pasta para as falas.")
        val result = File(folder, "fala_${UUID.randomUUID()}.wav")
        val part = File(folder, "${result.name}.part")
        val connection = URL("$endpoint/api/v1/tts").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 180_000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "audio/wav")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setFixedLengthStreamingMode(payload.size)
            connection.outputStream.use { it.write(payload) }
            val status = connection.responseCode
            if (status != 200) throw IOException(when (status) {
                401, 403 -> "Token do servidor recusado."
                413 -> "Áudio de referência ou texto acima do limite."
                429 -> "Servidor ocupado. Tente depois, manualmente."
                else -> "O serviço de voz respondeu HTTP $status. Confira os logs do servidor."
            })
            require(connection.contentType?.startsWith("audio/wav") == true ||
                connection.contentType?.startsWith("audio/x-wav") == true) {
                "Servidor não devolveu áudio WAV."
            }
            val declared = connection.contentLengthLong
            require(declared <= VoiceClonePolicy.MAX_OUTPUT) { "Resposta maior que 24 MiB." }
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        total += n
                        if (total > VoiceClonePolicy.MAX_OUTPUT) throw IOException("Resposta maior que 24 MiB.")
                        output.write(buffer, 0, n)
                    }
                    output.fd.sync()
                }
            }
            part.inputStream().use { input ->
                val bytes = ByteArray(12)
                require(input.read(bytes) == 12 && String(bytes, 0, 4) == "RIFF" &&
                    String(bytes, 8, 4) == "WAVE") { "O serviço não devolveu WAV válido." }
            }
            if (!part.renameTo(result)) throw IOException("Não foi possível salvar o áudio gerado.")
            result
        } catch (e: Exception) {
            result.delete()
            throw e
        } finally { part.delete(); connection.disconnect() }
    }
}

/** Conecta áudio gerado à fala, mantendo o modelo para distinguir Gemini/externo/clonado. */
internal object VoiceCloneAudio {
    fun isClone(track: AudioTrack?): Boolean = track?.kind == VoiceIaPolicy.KIND &&
        (track.ttsModel == VoiceClonePolicy.COQUI || track.ttsModel == VoiceClonePolicy.CHATTERBOX)

    fun prepare(file: File, project: VideoProject, cue: SubtitleCue,
        profile: VoiceCloneProfile): AudioTrack {
        require(VoiceClonePolicy.engineAllowed(profile.engine) && file.isFile)
        require(project.subtitles.any {
            it.id == cue.id && VoiceIaPolicy.fingerprint(it) == VoiceIaPolicy.fingerprint(cue)
        }) { "Legenda mudou. Gere novamente após revisar." }
        val extractor = MediaExtractor()
        val durationMs = try {
            extractor.setDataSource(file.absolutePath)
            val index = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IOException("Fala gerada não contém áudio reproduzível.")
            extractor.getTrackFormat(index).getLong(MediaFormat.KEY_DURATION) / 1000L
        } finally { extractor.release() }
        val old = project.audioTracks.firstOrNull { it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id }
        val start = old?.startMs ?: cue.startMs
        require(VoiceIaPolicy.fits(start, durationMs, project.durationMs)) {
            "Fala com ${durationMs}ms não cabe no vídeo. Reduza o texto ou ajuste o trecho."
        }
        return AudioTrack(uri = GeminiAudioFiles.uri(file),
            title = "Voz clonada · ${profile.name.take(30)}", startMs = start,
            durationMs = durationMs, volume = old?.volume ?: 1f,
            id = old?.id ?: UUID.randomUUID().toString(), kind = VoiceIaPolicy.KIND,
            subtitleId = cue.id, voiceId = profile.id, ttsModel = profile.engine,
            subtitleFingerprint = VoiceIaPolicy.fingerprint(cue))
    }
}
