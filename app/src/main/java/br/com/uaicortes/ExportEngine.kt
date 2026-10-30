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
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.VideoEncoderSettings
import androidx.media3.transformer.AudioEncoderSettings
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
    @Volatile private var isSavingToGallery = false

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
            DiagnosticRecorder.exception("export_media_read", e)
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
        DiagnosticRecorder.exception("media3_transformer", error)
        DiagnosticRecorder.failure("media3_code", "MEDIA3_${error.errorCode} ${error.message.orEmpty()}")
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

    /** Null quando a API Media3 não consegue estimar o percentual (sem inventar 0%). */
    fun progressOrNull(): Int? {
        val t = transformer ?: return null
        return if (t.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE)
            progressHolder.progress.coerceIn(0,100) else null
    }
    fun cancel() {
        // A cópia MediaStore precisa chegar ao fim, com rollback próprio em caso de erro.
        if (isSavingToGallery) return
        transformer?.cancel()
        transformer = null
        tempFile?.delete()
        tempFile = null
    }

    fun start(project: VideoProject, height: Int, fileName: String,
              onSaving: () -> Unit = {},
              onSuccess: (Uri) -> Unit, onError: (String) -> Unit) {
        require(!isSavingToGallery) { "Aguarde a conclusão do salvamento anterior." }
        require(project.clips.isNotEmpty()) { "Projeto sem vídeos" }
        val bitrateBps = VideoExportQuality.videoBitrateBps(height)
        require(context.cacheDir.usableSpace > VideoExportQuality.minimumFreeBytes(
            project.durationMs, height, true)) {
            "Espaço livre insuficiente para exportar em ${height}p com qualidade alta. " +
                "Libere armazenamento ou escolha 720p."
        }
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
            if (fadeIn > 0 || fadeOut > 0) {
                val entering = previous?.kind ?: TransitionEffects.CUT
                val leaving = next?.kind ?: TransitionEffects.CUT
                if (TransitionEffects.isMotion(entering) || TransitionEffects.isMotion(leaving))
                    videoEffects += ClipMotionTransitionEffect(clip.durationMs,
                        entering, fadeIn, leaving, fadeOut)
                if (!TransitionEffects.isMotion(entering) || !TransitionEffects.isMotion(leaving))
                    videoEffects += ClipColorTransitionEffect(clip.durationMs,
                        entering, fadeIn, leaving, fadeOut)
            }
            // Marca d'água e legendas são camadas INDEPENDENTES no projeto e no MP4.
            val textLayers = mutableListOf<androidx.media3.effect.TextureOverlay>()
            if (project.subtitles.isNotEmpty())
                textLayers += TimedSubtitleOverlay(project.subtitles, globalStartMs)
            project.textWatermark?.toSubtitle(project.durationMs)?.let { watermarkCue ->
                textLayers += TimedSubtitleOverlay(listOf(watermarkCue), globalStartMs)
            }
            if (textLayers.isNotEmpty()) videoEffects += OverlayEffect(textLayers)
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
        // Antes o bitrate ficava totalmente nas configurações automáticas do Media3.
        // Pedir mais bits reduz blocagem e perda de textura, sem mudar FPS/orientação.
        // Deixar fallback do codificador habilitado para aparelhos que não suportem a solicitação.
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder()
                .setBitrate(bitrateBps).build())
            .setRequestedAudioEncoderSettings(AudioEncoderSettings.Builder()
                .setBitrate(VideoExportQuality.AUDIO_BITRATE_BPS).build())
            .setEnableFallback(true)
            .build()
        transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    Log.i("UaiCortesExport", "MP4: ${exportResult.width}x${exportResult.height}, " +
                        "bitrate de vídeo medido=${exportResult.averageVideoBitrate} bps, " +
                        "solicitado=$bitrateBps bps, quadros=${exportResult.videoFrameCount}")
                    // Transformer terminou a renderização, mas a cópia MediaStore ainda não.
                    isSavingToGallery = true
                    onSaving()
                    Thread {
                        var savedUri: Uri? = null
                        var saveError: Exception? = null
                        try {
                            savedUri = saveToGallery(target, requestedName)
                        } catch (e: Exception) {
                            DiagnosticRecorder.exception("export_gallery", e)
                            saveError = e
                        } finally {
                            target.delete()
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                tempFile = null
                                transformer = null
                                isSavingToGallery = false
                                if (saveError != null) onError(saveError?.message ?: "Falha ao salvar vídeo")
                                else savedUri?.let(onSuccess)
                                    ?: onError("Não foi possível confirmar o vídeo salvo.")
                            }
                        }
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

    private fun saveToGallery(file: File, fileName: String): Uri =
        GalleryVideoSaver.save(context, file, fileName)

}
