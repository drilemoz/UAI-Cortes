package br.com.uaicortes

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File

/** Media3 writes an AAC audio-only MP4 container, saved as .m4a via SAF. */
// Opt-in is private to this implementation; callers must not inherit @UnstableApi.
@androidx.annotation.OptIn(UnstableApi::class)
class AudioExtractionEngine(private val context: Context) {
    private var transformer: Transformer? = null
    private var temporary: File? = null
    fun cancel() { transformer?.cancel(); transformer = null; temporary?.delete() }
    fun start(clips: List<Clip>, destination: Uri,
              onSuccess: () -> Unit, onError: (String) -> Unit) {
        require(clips.any { !it.isImage }) { "Nenhum clipe de vídeo com áudio foi selecionado." }
        cancel()
        // Fotos da linha do tempo não possuem áudio: o espaço temporal vira silêncio.
        val sequence = EditedMediaItemSequence.Builder(setOf(androidx.media3.common.C.TRACK_TYPE_AUDIO))
        clips.forEach { clip ->
            if (clip.isImage) sequence.addGap(clip.durationMs * 1000L)
            else sequence.addItem(EditedMediaItem.Builder(ClipMediaItems.playback(clip))
                .setDurationUs(clip.durationMs * 1000L).setRemoveVideo(true).build())
        }
        val composition = Composition.Builder(sequence.build()).build()
        val output = File(context.cacheDir, "uai-audio-${System.nanoTime()}.mp4")
        temporary = output
        transformer = Transformer.Builder(context).setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    Thread {
                        val result = runCatching {
                            context.contentResolver.openOutputStream(destination, "w")?.use { out ->
                                output.inputStream().use { it.copyTo(out) }
                            } ?: error("Não foi possível abrir o arquivo selecionado")
                        }
                        output.delete()
                        Handler(Looper.getMainLooper()).post {
                            result.fold({ onSuccess() }, { onError(it.message ?: "Erro ao salvar áudio") })
                        }
                    }.start()
                }
                override fun onError(composition: Composition, exportResult: ExportResult,
                                     exportException: ExportException) {
                    output.delete()
                    onError(exportException.message ?: "Falha na extração; confirme se a fonte contém áudio")
                }
            }).build()
        transformer!!.start(composition, output.absolutePath)
    }
}
