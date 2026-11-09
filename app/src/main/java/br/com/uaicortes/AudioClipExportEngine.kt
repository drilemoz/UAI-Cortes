package br.com.uaicortes

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
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
import java.io.InterruptedIOException
import java.util.concurrent.atomic.AtomicInteger

/** Recodifica somente o intervalo escolhido para AAC/M4A, sem alterar o original. */
@OptIn(UnstableApi::class)
internal class AudioClipExportEngine(private val context: Context) {
    private val generation = AtomicInteger(0)
    private var transformer: Transformer? = null
    private var temporary: File? = null
    private var worker: Thread? = null
    private var stoppedWorker: Thread? = null
    private val main = Handler(Looper.getMainLooper())

    fun cancel() {
        generation.incrementAndGet()
        transformer?.cancel()
        transformer = null
        stoppedWorker = worker?.also { it.interrupt() }
        worker = null
        temporary?.delete()
        temporary = null
    }

    /** Exclusão explícita do SAF apenas depois que qualquer cópia anterior fechar o fluxo. */
    fun removeIncomplete(destination: Uri, onResult: (Boolean) -> Unit) {
        val previous = stoppedWorker ?: worker
        Thread {
            val removed = runCatching {
                if (previous != null && previous !== Thread.currentThread()) {
                    previous.join(5_000L)
                    check(!previous.isAlive) { "A gravação ainda está terminando." }
                }
                if (DocumentsContract.isDocumentUri(context, destination))
                    DocumentsContract.deleteDocument(context.contentResolver, destination)
                else context.contentResolver.delete(destination, null, null) > 0
            }.getOrDefault(false)
            main.post { onResult(removed) }
        }.start()
    }

    fun export(source: Uri, durationMs: Long, startMs: Long, endMs: Long,
               destination: Uri, onPhase: (String) -> Unit,
               onDone: () -> Unit, onError: (String) -> Unit) {
        check(AudioClipSelectionPolicy.valid(durationMs, startMs, endMs)) { "Intervalo inválido." }
        cancel()
        val active = generation.get()
        fun isActive() = active == generation.get()
        val media = MediaItem.Builder().setUri(source).setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs).setEndPositionMs(endMs).build()
        ).build()
        val item = EditedMediaItem.Builder(media).setRemoveVideo(true).build()
        val sequence = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
            .addItem(item).build()
        val composition = Composition.Builder(sequence).build()
        val output = File(context.cacheDir, "uai-audio-corte-${System.nanoTime()}.mp4")
        temporary = output
        val engine = Transformer.Builder(context).setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (!isActive()) { output.delete(); return }
                    onPhase("Salvando arquivo M4A")
                    worker = Thread {
                        val result = runCatching {
                            check(output.length() > 0L) { "O recorte gerou áudio vazio." }
                            output.inputStream().use { input ->
                                context.contentResolver.openOutputStream(destination, "w")?.use { target ->
                                    val buffer = ByteArray(64 * 1024)
                                    while (true) {
                                        if (!isActive() || Thread.currentThread().isInterrupted)
                                            throw InterruptedIOException("Recorte cancelado")
                                        val count = input.read(buffer)
                                        if (count < 0) break
                                        target.write(buffer, 0, count)
                                    }
                                    target.flush()
                                } ?: error("O local de destino não permitiu gravar o áudio.")
                            }
                        }
                        output.delete()
                        main.post {
                            if (!isActive()) return@post
                            worker = null; transformer = null; temporary = null
                            result.fold({ onDone() }, { onError("Não foi possível salvar o M4A.") })
                        }
                    }.also { it.start() }
                }
                override fun onError(composition: Composition, exportResult: ExportResult,
                    exportException: ExportException) {
                    output.delete()
                    if (!isActive()) return
                    transformer = null; temporary = null
                    onError("Não foi possível gerar o trecho. Verifique o formato do áudio.")
                }
            }).build()
        transformer = engine
        onPhase("Recortando e codificando áudio")
        try { engine.start(composition, output.absolutePath) }
        catch (error: Exception) {
            output.delete(); transformer = null; temporary = null
            onError("Não foi possível iniciar a exportação.")
        }
    }
}
