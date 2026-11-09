package br.com.uaicortes

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.UUID

/** Áudio fornecido pelo usuário: vinculado à fala, nunca é rotulado como síntese IA. */
internal object VoiceIaExternalAudio {
    const val MODEL = "external"
    private const val MAX_BYTES = 64L * 1024 * 1024
    private val MIME_EXT = mapOf(
        "audio/wav" to "wav", "audio/x-wav" to "wav", "audio/wave" to "wav",
        "audio/vnd.wave" to "wav", "audio/x-pn-wav" to "wav",
        "audio/mpeg" to "mp3", "audio/mp3" to "mp3", "audio/mp4" to "m4a",
        "audio/x-m4a" to "m4a", "audio/mp4a-latm" to "m4a",
        "audio/aac" to "aac", "audio/ogg" to "ogg"
    )
    fun isImported(track: AudioTrack?): Boolean =
        track != null && track.kind == VoiceIaPolicy.KIND && track.ttsModel == MODEL

    /** Arquivo local já inserido conta como concluído, não como pedido pendente ao Gemini. */
    fun importedFor(project: VideoProject, cue: SubtitleCue): AudioTrack? =
        project.audioTracks.firstOrNull { isImported(it) && it.subtitleId == cue.id &&
            it.subtitleFingerprint == VoiceIaPolicy.fingerprint(cue) &&
            runCatching { File(URI(it.uri)).isFile }.getOrDefault(false) }

    suspend fun prepare(context: Context, source: Uri, project: VideoProject,
        cue: SubtitleCue): Pair<AudioTrack, File> = withContext(Dispatchers.IO) {
        require(source.scheme == "content") { "Escolha um arquivo pelo seletor Android." }
        require(project.subtitles.any { it.id == cue.id && VoiceIaPolicy.fingerprint(it) ==
            VoiceIaPolicy.fingerprint(cue) }) { "A legenda foi alterada. Selecione novamente." }
        val mime = context.contentResolver.getType(source)?.lowercase().orEmpty()
        val extension = MIME_EXT[mime] ?: throw IOException(
            "Formato não reconhecido. Escolha WAV, MP3, M4A, AAC ou OGG.")
        val declared = context.contentResolver.query(source,
            arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getColumnIndex(OpenableColumns.SIZE)
                .takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getLong)
            else null
        }
        require(declared == null || declared in 1..MAX_BYTES) {
            "O arquivo precisa ter no máximo 64 MiB."
        }
        val folder = File(context.filesDir, "voice_ia_imports").apply {
            if (!exists() && !mkdirs()) throw IOException("Não foi possível criar a pasta privada.")
        }
        if (StatFs(folder.path).availableBytes < (declared ?: MAX_BYTES) + 2 * 1024 * 1024L)
            throw IOException("Espaço insuficiente para copiar a fala.")
        val file = File(folder, "fala_${UUID.randomUUID()}.$extension")
        val part = File(folder, "${file.name}.part")
        try {
            val input = context.contentResolver.openInputStream(source)
                ?: throw IOException("Não foi possível abrir o áudio selecionado.")
            input.use { stream ->
                part.outputStream().use { out ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_BYTES) throw IOException("O áudio ultrapassa 64 MiB.")
                        out.write(buffer, 0, read)
                    }
                    if (total == 0L) throw IOException("O áudio selecionado está vazio.")
                    out.fd.sync()
                }
            }
            val extractor = MediaExtractor()
            val durationMs = try {
                extractor.setDataSource(part.absolutePath)
                val audioTrack = (0 until extractor.trackCount).firstOrNull { i ->
                    extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                        ?.startsWith("audio/") == true
                } ?: throw IOException("O arquivo não contém áudio reproduzível.")
                val format = extractor.getTrackFormat(audioTrack)
                if (!format.containsKey(MediaFormat.KEY_DURATION))
                    throw IOException("Não foi possível medir a duração do áudio.")
                format.getLong(MediaFormat.KEY_DURATION) / 1000L
            } finally { extractor.release() }
            val old = project.audioTracks.firstOrNull {
                it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id
            }
            val start = old?.startMs ?: cue.startMs
            require(VoiceIaPolicy.fits(start, durationMs, project.durationMs)) {
                "A fala não cabe até o fim do projeto. Ajuste o áudio externamente."
            }
            if (!part.renameTo(file)) throw IOException("Falha ao finalizar a cópia privada.")
            val replacement = AudioTrack(
                uri = GeminiAudioFiles.uri(file),
                title = "Áudio externo · ${cue.text.trim().take(30)}",
                startMs = start, durationMs = durationMs,
                volume = old?.volume ?: 1f, id = old?.id ?: UUID.randomUUID().toString(),
                kind = VoiceIaPolicy.KIND, subtitleId = cue.id, ttsModel = MODEL,
                subtitleFingerprint = VoiceIaPolicy.fingerprint(cue)
            )
            replacement to file
        } catch (e: Exception) {
            file.delete()
            throw e
        } finally { part.delete() }
    }
}
