package br.com.uaicortes

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
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
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: error("Não foi possível ler a imagem escolhida.")
        require(options.outWidth > 0 && options.outHeight > 0 &&
            options.outWidth.toLong() * options.outHeight.toLong() <= MAX_PIXELS) {
            "Imagem inválida ou acima de 24 megapixels. Reduza a resolução."
        }
        val title = runCatching { mediaName(context, uri) }.getOrDefault("Imagem")
        ImageClipSource(uri.toString(), title, mime)
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
