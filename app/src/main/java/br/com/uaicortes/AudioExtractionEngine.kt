package br.com.uaicortes

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
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

/** Media3 writes an AAC audio-only MP4 container, saved as .m4a via SAF. */
// Opt-in is private to this implementation; callers must not inherit @UnstableApi.
@androidx.annotation.OptIn(UnstableApi::class)
class AudioExtractionEngine(private val context: Context) {
    private var transformer: Transformer? = null
    private var temporary: File? = null
    private var copyWorker: Thread? = null
    private var stoppedCopyWorker: Thread? = null
    private val generation = AtomicInteger(0)

    fun cancel() {
        // Descarta callbacks atrasados do Transformer e de salvamentos anteriores.
        generation.incrementAndGet()
        transformer?.cancel(); transformer = null
        stoppedCopyWorker = copyWorker?.also { it.interrupt() }
        copyWorker = null
        temporary?.delete(); temporary = null
    }

    /** Exclui apenas a saída incompleta solicitada explicitamente pelo usuário.
     * Aguarda a cópia anterior fechar o OutputStream, evitando apagar com escrita ativa.
     * Pode falhar em provedores SAF que não permitem exclusão.
     */
    fun removeIncompleteDestination(destination: Uri, onResult: (Boolean, String?) -> Unit) {
        val previousWorker = stoppedCopyWorker ?: copyWorker
        val main = Handler(Looper.getMainLooper())
        Thread {
            val deletion = runCatching {
                if (previousWorker != null && previousWorker !== Thread.currentThread()) {
                    previousWorker.join(5_000)
                    check(!previousWorker.isAlive) {
                        "A gravação ainda está encerrando. Tente novamente em instantes."
                    }
                }
                val resolver = context.contentResolver
                val removed = if (DocumentsContract.isDocumentUri(context, destination)) {
                    DocumentsContract.deleteDocument(resolver, destination)
                } else {
                    resolver.delete(destination, null, null) > 0
                }
                check(removed) { "O provedor de arquivos não permitiu excluir o documento." }
                true
            }
            main.post {
                onResult(deletion.getOrDefault(false), deletion.exceptionOrNull()?.message)
            }
        }.start()
    }

    fun start(clips: List<Clip>, destination: Uri,
              onSuccess: () -> Unit, onError: (String) -> Unit,
              onPhase: (String) -> Unit = {}) {
        require(clips.any { !it.isImage }) { "Nenhum clipe de vídeo com áudio foi selecionado." }
        cancel()
        val session = generation.get()
        fun isCurrent() = generation.get() == session
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
        val main = Handler(Looper.getMainLooper())
        transformer = Transformer.Builder(context).setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (!isCurrent()) { output.delete(); return }
                    onPhase("Salvando arquivo M4A")
                    val worker = Thread {
                        val result = runCatching {
                            if (output.length() <= 0L) error("A extração gerou um arquivo vazio.")
                            if (!isCurrent()) throw InterruptedIOException("Extração cancelada")
                            output.inputStream().use { input ->
                                context.contentResolver.openOutputStream(destination, "w")?.use { out ->
                                    val buffer = ByteArray(64 * 1024)
                                    while (true) {
                                        if (!isCurrent() || Thread.currentThread().isInterrupted)
                                            throw InterruptedIOException("Extração cancelada")
                                        val count = input.read(buffer)
                                        if (count < 0) break
                                        out.write(buffer, 0, count)
                                    }
                                    out.flush()
                                    if (!isCurrent()) throw InterruptedIOException("Extração cancelada")
                                } ?: error("Não foi possível abrir o arquivo selecionado")
                            }
                        }
                        output.delete()
                        main.post {
                            if (!isCurrent()) return@post
                            copyWorker = null; transformer = null; temporary = null
                            result.fold({ onSuccess() },
                                { onError(it.message ?: "Erro ao salvar áudio") })
                        }
                    }
                    copyWorker = worker
                    worker.start()
                }
                override fun onError(composition: Composition, exportResult: ExportResult,
                                     exportException: ExportException) {
                    output.delete()
                    if (!isCurrent()) return
                    transformer = null; temporary = null
                    onError(exportException.message ?: "Falha na extração; confirme se a fonte contém áudio")
                }
            }).build()
        onPhase("Extraindo faixa de áudio")
        transformer!!.start(composition, output.absolutePath)
    }
}
