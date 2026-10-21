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
import androidx.media3.common.OverlaySettings
import androidx.media3.effect.Presentation
import androidx.media3.effect.OverlayEffect
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
import androidx.media3.common.VideoCompositorSettings
import android.net.Uri
import android.util.Log
import java.io.FileNotFoundException
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
class PipCompositor(private val overlays: List<Overlay>) : VideoCompositorSettings {
    private data class Placement(val scaleX: Float, val scaleY: Float, val x: Float, val y: Float)
    private var computed: List<Placement> = emptyList()

    override fun getOutputSize(inputSizes: MutableList<Size>): Size {
        // A ordem das sequências de VÍDEO é overlays[0..n-1], depois o vídeo principal.
        val main = inputSizes.getOrElse(overlays.size) { inputSizes.last() }
        val outW = main.width.coerceAtLeast(1).toFloat()
        val outH = main.height.coerceAtLeast(1).toFloat()
        computed = overlays.mapIndexed { index, pip ->
            val input = inputSizes[index]
            val inW = input.width.coerceAtLeast(1).toFloat()
            val inH = input.height.coerceAtLeast(1).toFloat()
            val (wf, hf) = OverlayEditorLogic.dimensions(pip.scale, inW / inH, outW / outH)
            Placement(wf * outW / inW, hf * outH / inH,
                OverlayEditorLogic.clampCenter(pip.x, wf),
                OverlayEditorLogic.clampCenter(pip.y, hf))
        }
        return main
    }

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings {
        val pip = overlays.getOrNull(inputId) ?: return StaticOverlaySettings.Builder().build()
        val placement = computed.getOrNull(inputId) ?: return StaticOverlaySettings.Builder()
            .setAlphaScale(0f).build()
        return StaticOverlaySettings.Builder()
            .setScale(placement.scaleX, placement.scaleY)
            .setOverlayFrameAnchor(0f, 0f)
            .setBackgroundFrameAnchor(placement.x * 2f - 1f, 1f - placement.y * 2f)
            .setAlphaScale(if (presentationTimeUs >= pip.startAtMs * 1000L &&
                presentationTimeUs < (pip.startAtMs + pip.durationMs) * 1000L) 1f else 0f)
            .build()
    }
}

@OptIn(UnstableApi::class)
class ExportEngine(private val context: Context) {
    private var transformer: Transformer? = null
    private var tempFile: File? = null
    private val progressHolder = ProgressHolder()

    /** Não comece a codificação quando um arquivo local perdeu a permissão de leitura. */
    private fun checkReadable(uriText: String, description: String) {
        val uri = Uri.parse(uriText)
        if (uri.scheme != "content" && uri.scheme != "file") {
            throw IllegalArgumentException("$description: endereço de mídia incompatível com a exportação local")
        }
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { file ->
                if (file.statSize == 0L) throw FileNotFoundException("arquivo vazio")
            } ?: throw FileNotFoundException("não foi possível abrir o arquivo")
        } catch (e: Exception) {
            Log.e("UaiCortesExport", "Não foi possível abrir $description para exportação", e)
            throw IllegalStateException("$description não está acessível. Selecione novamente o arquivo original.", e)
        }
    }

    private fun validateSources(project: VideoProject) {
        for ((index, clip) in project.clips.withIndex()) {
            require(ExportTimingPolicy.validClip(clip.startMs, clip.endMs, clip.sourceDurationMs)) {
                "Trecho ${index + 1} possui início/fim inválidos; revise o corte antes de exportar."
            }
            checkReadable(clip.uri, "Vídeo ${index + 1}")
        }
        project.overlays.forEachIndexed { index, overlay ->
            checkReadable(overlay.uri, "PiP ${index + 1}")
        }
        project.audioTracks.forEachIndexed { index, audio ->
            if (audio.volume > 0f) checkReadable(audio.uri, "Áudio ${index + 1}")
        }
        project.music?.takeIf { it.volume > 0f }?.let { checkReadable(it.uri, "Música") }
        require(context.cacheDir.usableSpace >= 32L * 1024L * 1024L) {
            "Pouco espaço livre para gerar o MP4 temporário. Libere armazenamento e tente novamente."
        }
    }

    private fun describeExportError(error: ExportException): String {
        Log.e("UaiCortesExport", "Transformer: código ${error.errorCode}", error)
        val hint = when (error.errorCode) {
            ExportException.ERROR_CODE_IO_NO_PERMISSION, ExportException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "Sem acesso a uma mídia do projeto. Selecione o arquivo novamente."
            ExportException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ->
                "O corte ultrapassa um trecho disponível na mídia. Revise os horários."
            ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED, ExportException.ERROR_CODE_DECODER_INIT_FAILED ->
                "O aparelho não conseguiu decodificar uma mídia. Teste outro arquivo ou formato."
            ExportException.ERROR_CODE_ENCODER_INIT_FAILED, ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED ->
                "O aparelho não conseguiu iniciar a codificação H.264/AAC. Tente 720p."
            ExportException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED ->
                "Falha ao compor um quadro de vídeo (legendas, PiP ou efeito). Verifique as camadas do projeto."
            else -> "Não foi possível processar uma mídia do projeto. Verifique os arquivos e os cortes."
        }
        val detail = generateSequence(error as Throwable?) { it.cause }
            .lastOrNull()?.message?.take(160)?.replace('\n', ' ')
            ?.takeIf { it.isNotBlank() && it != error.message }
        return "$hint (${error.errorCodeName})" + (detail?.let { " Detalhe: $it" } ?: "")
    }

    fun progress(): Int {
        val t = transformer ?: return 0
        return if (t.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE)
            progressHolder.progress.coerceIn(0, 100) else 0
    }
    fun cancel() {
        transformer?.cancel()
        transformer = null
        tempFile?.delete()
        tempFile = null
    }

    fun start(project: VideoProject, height: Int, fileName: String,
              onSuccess: (Uri) -> Unit, onError: (String) -> Unit) {
        require(project.clips.isNotEmpty()) { "Projeto sem vídeos" }
        require(project.overlays.size <= OverlayPlacement.MAX_PIPS) { "O limite é de 4 PiPs" }
        cancel()
        validateSources(project)
        var globalStartMs = 0L
        val mainItems = project.clips.mapIndexed { index, clip ->
            val item = MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs).build()).build()
            val previous = EditFeatures.boundary(project, index - 1)
            val next = EditFeatures.boundary(project, index)
            val fadeIn = EditFeatures.transitionMs(previous,
                project.clips.getOrElse(index - 1) { clip }.durationMs, clip.durationMs)
            val fadeOut = EditFeatures.transitionMs(next, clip.durationMs,
                project.clips.getOrElse(index + 1) { clip }.durationMs)
            val videoEffects = mutableListOf<androidx.media3.common.Effect>()
            if (fadeIn > 0 || fadeOut > 0) videoEffects += ClipFadeEffect(clip.durationMs,
                fadeIn, fadeOut, previous?.kind == "Fade branco", next?.kind == "Fade branco")
            if (project.subtitles.isNotEmpty()) videoEffects += OverlayEffect(
                listOf(TimedSubtitleOverlay(project.subtitles, globalStartMs)))
            globalStartMs += clip.durationMs
            // Media3: duração de EditedMediaItem é a duração ORIGINAL, não do recorte.
            // Para vídeo com duração intrínseca, Transformer a descobre; omitir evita
            // informar incorretamente clip.durationMs como duração da fonte.
            EditedMediaItem.Builder(item)
                .setEffects(Effects(if (clip.volume == 1f) emptyList() else listOf(GainProcessor(clip.volume)),
                    videoEffects)).build()
        }
        val mainSequence = EditedMediaItemSequence.withAudioAndVideoFrom(mainItems)
        val sequences = mutableListOf<EditedMediaItemSequence>()
        // Ordem estável das entradas de vídeo; áudio é adicionado DEPOIS do vídeo principal.
        for (pip in project.overlays.take(OverlayPlacement.MAX_PIPS)) {
            val media = if (pip.isImage) MediaItem.Builder().setUri(pip.uri)
                .setImageDurationMs(pip.durationMs).build()
                else MediaItem.Builder().setUri(pip.uri).setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder().setEndPositionMs(pip.durationMs).build()).build()
            val edited = EditedMediaItem.Builder(media)
                .apply { if (pip.isImage) setFrameRate(30) }.build()
            val overlayTrack = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_VIDEO))
            if (pip.startAtMs > 0) overlayTrack.addGap(pip.startAtMs * 1000L)
            overlayTrack.addItem(edited)
            val trailing = project.durationMs - pip.startAtMs - pip.durationMs
            if (trailing > 0) overlayTrack.addGap(trailing * 1000L)
            sequences += overlayTrack.build()
        }
        sequences += mainSequence
        for (pip in project.overlays.take(OverlayPlacement.MAX_PIPS)) {
            if (pip.isImage || pip.muted || pip.volume <= 0f) continue
            val media = MediaItem.Builder().setUri(pip.uri).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setEndPositionMs(pip.durationMs).build()).build()
            val audioTrack = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
            if (pip.startAtMs > 0) audioTrack.addGap(pip.startAtMs * 1000L)
            audioTrack.addItem(EditedMediaItem.Builder(media)
                .setEffects(Effects(listOf(GainProcessor(pip.volume)), emptyList())).build())
            sequences += audioTrack.build()
        }
        project.audioTracks.forEach { audio ->
            val start = audio.startMs.coerceIn(0L, project.durationMs)
            val sourceDuration = mediaDuration(context, Uri.parse(audio.uri))
            val sourceStart = audio.sourceStartMs.coerceIn(0L, sourceDuration)
            val length = audio.durationMs.takeIf { it > 0 } ?: (sourceDuration - sourceStart)
            val playable = minOf(length, sourceDuration - sourceStart, project.durationMs - start)
            if (playable > 0) {
                val track = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
                if (start > 0) track.addGap(start * 1000L)
                track.addItem(EditedMediaItem.Builder(MediaItem.Builder().setUri(audio.uri)
                    .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(sourceStart)
                        .setEndPositionMs(sourceStart + playable).build()).build())
                    .setEffects(Effects(listOf(GainProcessor(audio.volume)), emptyList())).build())
                val trailing = project.durationMs - start - playable
                if (trailing > 0) track.addGap(trailing * 1000L)
                sequences += track.build()
            }
        }
        project.music?.let { music ->
            val track = EditedMediaItem.Builder(MediaItem.fromUri(music.uri))
                .setEffects(Effects(listOf(GainProcessor(music.volume)), emptyList())).build()
            sequences += EditedMediaItemSequence.withAudioFrom(listOf(track))
                .buildUpon().setIsLooping(true).build()
        }
        val composition = Composition.Builder(sequences).apply {
            if (project.overlays.isNotEmpty()) setVideoCompositorSettings(PipCompositor(project.overlays.take(4)))
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
                        } finally { target.delete(); tempFile = null; transformer = null }
                    }.start()
                }
                override fun onError(composition: Composition, exportResult: ExportResult,
                                     exportException: ExportException) {
                    target.delete()
                    tempFile = null
                    transformer = null
                    onError(describeExportError(exportException))
                }
            }).build()
        transformer!!.start(composition, target.absolutePath)
    }

    private fun saveToGallery(file: File, fileName: String): Uri {
        require(file.isFile && file.length() > 0L) {
            "O arquivo MP4 temporário está vazio ou não foi gerado."
        }
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
            val bytesCopied = resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } ?: error("Não foi possível gravar o vídeo")
            check(bytesCopied == file.length()) {
                "A cópia do MP4 para a galeria terminou antes do esperado."
            }
            check(resolver.update(uri, ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }, null, null) > 0) { "Não foi possível finalizar o vídeo na galeria" }
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}
