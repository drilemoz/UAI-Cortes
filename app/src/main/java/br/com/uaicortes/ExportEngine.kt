package br.com.uaicortes

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.Presentation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoCompositorSettings
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
class GainProcessor(private val gain: Float) : BaseAudioProcessor() {
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT)
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        return inputAudioFormat
    }
    override fun queueInput(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining()).order(ByteOrder.nativeOrder())
        inputBuffer.order(ByteOrder.nativeOrder())
        while (inputBuffer.remaining() >= 2) {
            val sample = inputBuffer.short.toInt()
            out.putShort((sample * gain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
        }
        out.flip()
    }
}

@OptIn(UnstableApi::class)
class PipCompositor(private val pip: Overlay) : VideoCompositorSettings {
    override fun getOutputSize(inputSizes: MutableList<Size>): Size = inputSizes.getOrElse(1) { inputSizes[0] }
    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings {
        return if (inputId == 0) StaticOverlaySettings.Builder()
            .setScale(pip.scale, pip.scale)
            .setOverlayFrameAnchor(0f, 0f)
            .setBackgroundFrameAnchor(pip.x * 2f - 1f, 1f - pip.y * 2f)
            .setAlphaScale(if (presentationTimeUs < pip.startAtMs * 1000L || presentationTimeUs >= (pip.startAtMs + pip.durationMs) * 1000L) 0f else 1f)
            .build()
        else StaticOverlaySettings.Builder().build()
    }
}

@OptIn(UnstableApi::class)
class ExportEngine(private val context: Context) {
    private var transformer: Transformer? = null
    private var tempFile: File? = null
    private val progressHolder = ProgressHolder()

    fun progress(): Int {
        val t = transformer ?: return 0
        return if (t.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE)
            progressHolder.progress.coerceIn(0, 100) else 0
    }
    fun cancel() {
        transformer?.cancel()
        transformer = null
        tempFile?.delete()
    }

    fun start(project: VideoProject, height: Int, fileName: String,
              onSuccess: (Uri) -> Unit, onError: (String) -> Unit) {
        require(project.clips.isNotEmpty()) { "Projeto sem vídeos" }
        cancel()
        val mainItems = project.clips.map { clip ->
            val item = MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs).build()).build()
            EditedMediaItem.Builder(item).setDurationUs(clip.durationMs * 1000L)
                .setEffects(Effects(listOf(GainProcessor(clip.volume)), emptyList())).build()
        }
        val mainSequence = EditedMediaItemSequence.withAudioAndVideoFrom(mainItems)
        val sequences = mutableListOf<EditedMediaItemSequence>()
        project.pip?.let { pip ->
            val media = if (pip.isImage) MediaItem.Builder().setUri(pip.uri)
                .setImageDurationMs(pip.durationMs).build()
                else MediaItem.Builder().setUri(pip.uri).setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder().setEndPositionMs(pip.durationMs).build()).build()
            val edited = EditedMediaItem.Builder(media).setDurationUs(pip.durationMs * 1000L)
                .apply { if (pip.isImage) setFrameRate(30) }.build()
            val overlayTrack = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_VIDEO))
            if (pip.startAtMs > 0) overlayTrack.addGap(pip.startAtMs * 1000L)
            overlayTrack.addItem(edited)
            val trailing = project.durationMs - pip.startAtMs - pip.durationMs
            if (trailing > 0) overlayTrack.addGap(trailing * 1000L)
            sequences += overlayTrack.build()
            if (!pip.isImage && pip.volume > 0f) {
                val audioTrack = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
                if (pip.startAtMs > 0) audioTrack.addGap(pip.startAtMs * 1000L)
                audioTrack.addItem(EditedMediaItem.Builder(media)
                    .setDurationUs(pip.durationMs * 1000L)
                    .setEffects(Effects(listOf(GainProcessor(pip.volume)), emptyList())).build())
                sequences += audioTrack.build()
            }
        }
        // O compositor desenha a primeira sequência sobre a segunda.
        // A faixa PiP vem primeiro, mas inclui lacunas transparentes até a duração principal.
        sequences.add(if (project.pip != null) 1 else 0, mainSequence)
        project.music?.let { music ->
            val track = EditedMediaItem.Builder(MediaItem.fromUri(music.uri))
                .setEffects(Effects(listOf(GainProcessor(music.volume)), emptyList())).build()
            sequences += EditedMediaItemSequence.withAudioFrom(listOf(track))
                .buildUpon().setIsLooping(true).build()
        }
        val composition = Composition.Builder(sequences).apply {
            if (project.pip != null) setVideoCompositorSettings(PipCompositor(project.pip))
            setEffects(Effects(emptyList(), listOf(Presentation.createForHeight(height))))
        }.build()
        val target = File(context.cacheDir, "uai-export-${System.currentTimeMillis()}.mp4")
        tempFile = target
        val requestedName = fileName.trim().let { if (it.endsWith(".mp4", true)) it else "$it.mp4" }
        transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    Thread {
                        try {
                            val uri = saveToGallery(target, requestedName)
                            android.os.Handler(android.os.Looper.getMainLooper()).post { onSuccess(uri) }
                        } catch (e: Exception) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post { onError(e.message ?: "Falha ao salvar vídeo") }
                        } finally { target.delete() }
                    }.start()
                }
                override fun onError(composition: Composition, exportResult: ExportResult,
                                     exportException: ExportException) {
                    target.delete()
                    onError(exportException.message ?: "Falha durante a exportação")
                }
            }).build()
        transformer!!.start(composition, target.absolutePath)
    }

    private fun saveToGallery(file: File, fileName: String): Uri {
        // Android 10+ permite MediaStore sem permissão global de arquivos.
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/UaiCortes")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Não foi possível criar o arquivo na galeria")
        try {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: error("Não foi possível gravar o vídeo")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}
