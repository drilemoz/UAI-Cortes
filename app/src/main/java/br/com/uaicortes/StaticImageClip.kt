package br.com.uaicortes

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Uma foto na faixa principal equivale a UM clipe, não a um PiP ou vídeo intermediário. */
internal object ImageClipPolicy {
    const val DEFAULT_SECONDS = 3
    const val MIN_SECONDS = 1
    const val MAX_SECONDS = 30

    enum class Placement { START, BEFORE_SELECTED, AFTER_SELECTED, END }

    fun seconds(value: Int): Int = value.coerceIn(MIN_SECONDS, MAX_SECONDS)

    fun create(uri: String, title: String, mimeType: String, seconds: Int = DEFAULT_SECONDS): Clip {
        require(uri.startsWith("content://")) { "Selecione a foto pelo armazenamento Android." }
        require(mimeType in ALLOWED_MIMES) { "Formato de imagem não suportado." }
        val duration = seconds(seconds) * 1000L
        return Clip(uri = uri, title = title.ifBlank { "Imagem" },
            sourceDurationMs = duration, startMs = 0L, endMs = duration,
            volume = 0f, isImage = true, imageMimeType = mimeType)
    }

    fun changeDuration(clip: Clip, requestedSeconds: Int): Clip {
        require(clip.isImage) { "A duração fixa é exclusiva de imagens." }
        val duration = seconds(requestedSeconds) * 1000L
        return clip.copy(sourceDurationMs = duration, startMs = 0L, endMs = duration)
    }

    fun index(clips: List<Clip>, selectedId: String?, placement: Placement): Int {
        val selected = clips.indexOfFirst { it.id == selectedId }
        return when (placement) {
            Placement.START -> 0
            Placement.BEFORE_SELECTED -> if (selected >= 0) selected else 0
            Placement.AFTER_SELECTED -> if (selected >= 0) selected + 1 else clips.size
            Placement.END -> clips.size
        }
    }

    fun insert(clips: List<Clip>, clip: Clip, placement: Placement,
        selectedId: String? = null): List<Clip> = clips.toMutableList().apply {
        add(index(clips, selectedId, placement), clip)
    }

    /** Preserva legendas/voz/PiP com tempo absoluto quando uma capa é inserida.
     * Música de fundo, por definição, continua no tempo zero; áudio preso a um clipe
     * é resolvido pelo ID do clipe e por isso não deve receber um segundo deslocamento.
     */
    fun insertIntoProject(project: VideoProject, clip: Clip, placement: Placement,
        selectedId: String? = null): VideoProject {
        require(clip.isImage) { "Insira um clipe de imagem." }
        val at = index(project.clips, selectedId, placement)
        val begin = project.clips.take(at).sumOf { it.durationMs }
        val shifted = shiftTimedElements(project, begin, clip.durationMs)
        val changedClips = project.clips.toMutableList().apply { add(at, clip) }
        return EditFeatures.repositionAfterDelete(shifted, changedClips)
    }

    /** Alterar a duração da foto mantém o sincronismo do material posterior. */
    fun resizeInProject(project: VideoProject, clipId: String, requestedSeconds: Int): VideoProject {
        val at = project.clips.indexOfFirst { it.id == clipId }
        require(at >= 0 && project.clips[at].isImage) { "Selecione uma imagem válida." }
        val previous = project.clips[at]
        val resized = changeDuration(previous, requestedSeconds)
        val delta = resized.durationMs - previous.durationMs
        val oldEnd = project.clips.take(at + 1).sumOf { it.durationMs }
        val shifted = shiftTimedElements(project, oldEnd, delta)
        return shifted.copy(clips = project.clips.mapIndexed { i, c -> if (i == at) resized else c })
    }

    private fun shiftTimedElements(project: VideoProject, at: Long, delta: Long): VideoProject {
        if (delta == 0L) return project
        fun shift(time: Long): Long = if (time >= at) (time + delta).coerceAtLeast(0L) else time
        return project.copy(
            subtitles = project.subtitles.map { cue ->
                if (cue.startMs < at) cue else cue.copy(
                    startMs = shift(cue.startMs), endMs = shift(cue.endMs),
                    wordTimings = cue.wordTimings.map { w ->
                        w.copy(startMs = shift(w.startMs), endMs = shift(w.endMs))
                    })
            },
            overlays = project.overlays.map { it.copy(startAtMs = shift(it.startAtMs)) },
            audioTracks = project.audioTracks.map { track ->
                if (track.clipId == null) track.copy(startMs = shift(track.startMs)) else track
            }
        )
    }

    val ALLOWED_MIMES = setOf("image/jpeg", "image/png", "image/webp",
        "image/heic", "image/heif", "image/bmp")
}

internal data class ImageClipSource(val uri: String, val title: String, val mimeType: String)

/** Só cabeçalhos/limites: não decodificar a foto inteira na thread principal. */
internal object ImageClipImport {
    private const val MAX_FILE_BYTES = 24L * 1024L * 1024L
    private const val MAX_PIXELS = 24_000_000L

    suspend fun inspect(context: Context, uri: Uri): ImageClipSource = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") { "Escolha uma imagem da galeria ou do gerenciador de arquivos." }
        val mime = context.contentResolver.getType(uri)?.lowercase()
            ?: error("Não foi possível reconhecer o formato da imagem.")
        require(mime in ImageClipPolicy.ALLOWED_MIMES) {
            "Formato não suportado. Escolha JPG, PNG, WebP, HEIC ou BMP (GIF não é aceito)."
        }
        if (mime == "image/heic" || mime == "image/heif") {
            require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                "HEIC exige Android 9 ou mais recente. Escolha JPG ou PNG."
            }
        }
        val length = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE),
            null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }
        require(length == null || length in 1..MAX_FILE_BYTES) {
            "Imagem vazia ou acima de 24 MB. Escolha um arquivo menor."
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // Com inJustDecodeBounds=true, decodeStream() retorna null mesmo para imagem
        // válida. A validação é feita pelas dimensões em options, NÃO pelo retorno.
        val imageStream = context.contentResolver.openInputStream(uri)
            ?: error("Não foi possível abrir a imagem escolhida.")
        imageStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        require(options.outWidth > 0 && options.outHeight > 0 &&
            options.outWidth.toLong() * options.outHeight.toLong() <= MAX_PIXELS) {
            "Imagem inválida ou acima de 24 megapixels. Reduza a resolução."
        }
        val title = runCatching { mediaName(context, uri) }.getOrDefault("Imagem")
        ImageClipSource(uri.toString(), title, mime)
    }

    /** Cópia durável: galeria MediaStore não oferece grant persistível via SAF.
     * O clipe aponta para conteúdo privado do Uai Cortes, não para uma URI temporária.
     */
    suspend fun copyIntoProject(context: Context, source: ImageClipSource): ImageClipSource =
        withContext(Dispatchers.IO) {
            val extension = when (source.mimeType) {
                "image/jpeg" -> "jpg"; "image/png" -> "png"; "image/webp" -> "webp"
                "image/heic" -> "heic"; "image/heif" -> "heif"; else -> "bmp"
            }
            val dir = File(context.filesDir, "project_images_v1")
            require(dir.isDirectory || dir.mkdirs()) { "Não foi possível preparar o armazenamento." }
            val output = File(dir, UUID.randomUUID().toString() + "." + extension)
            val partial = File(dir, output.name + ".partial")
            try {
                val inputUri = Uri.parse(source.uri)
                var written = 0L
                context.contentResolver.openInputStream(inputUri)?.use { input ->
                    partial.outputStream().use { target ->
                        val buffer = ByteArray(32 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            written += count
                            require(written <= 24L * 1024L * 1024L) { "Imagem acima de 24 MB." }
                            target.write(buffer, 0, count)
                        }
                        target.fdSyncIfAvailable()
                    }
                } ?: error("Não foi possível ler a imagem.")
                require(written > 0L && partial.renameTo(output)) { "Falha ao salvar imagem no projeto." }
                val stable = FileProvider.getUriForFile(context,
                    context.packageName + ".diagnostics", output)
                ImageClipSource(stable.toString(), source.title, source.mimeType)
            } catch (failure: Exception) {
                partial.delete(); output.delete(); throw failure
            }
        }

    /** Exclui somente cópia PRIVADA ainda não inserida. Nunca remove a foto original. */
    fun discardUnused(context: Context, source: ImageClipSource?) {
        val uri = source?.uri?.let(Uri::parse) ?: return
        if (uri.scheme != "content" || uri.authority != context.packageName + ".diagnostics") return
        if (uri.pathSegments.firstOrNull() != "project_images") return
        val fileName = uri.pathSegments.lastOrNull() ?: return
        if (!fileName.matches(Regex("[0-9a-f-]{36}\\.(jpg|png|webp|heic|heif|bmp)"))) return
        val output = File(File(context.filesDir, "project_images_v1"), fileName)
        runCatching { output.delete() }
    }

    private fun java.io.OutputStream.fdSyncIfAvailable() {
        if (this is java.io.FileOutputStream) fd.sync()
    }
}

/** Uma única configuração de mídia para todas as prévias; exibição da foto via PlayerView. */
internal object ClipMediaItems {
    @OptIn(UnstableApi::class)
    fun playback(clip: Clip): MediaItem {
        val item = MediaItem.Builder().setUri(clip.uri)
        return if (clip.isImage) item
            .apply { clip.imageMimeType?.let { setMimeType(it) } }
            .setImageDurationMs(clip.durationMs).build()
        else item.setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(clip.startMs).setEndPositionMs(clip.endMs).build()).build()
    }
}
