package br.com.uaicortes

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Renderiza uma janela curta via Media3; extrai o quadro já composto, sem screenshot.
 * O MP4 é temporário, e só o PNG final é publicado na galeria do Android.
 */
internal object FrameSnapshotRenderer {
    suspend fun save(context: Context, job: FrameSnapshotJob, fileName: String): Uri {
        if (job.directSourceFrame) {
            // Sem PiP, legenda nem marca d'água no instante: decodificar diretamente
            // o quadro original evita recodificação, muxer e perda de qualidade.
            return withContext(Dispatchers.IO) {
                VideoFrameSaver.save(context.applicationContext, job.sourceFrame, fileName)
            }
        }
        return suspendCancellableCoroutine { continuation ->
            val engine = ExportEngine(context.applicationContext)
            continuation.invokeOnCancellation {
                Handler(Looper.getMainLooper()).post { engine.cancel() }
            }
            try {
                engine.start(job.project, job.requestedShortSide,
                    fileName = "snapshot-frame.mp4",
                    preset = ExportBitratePreset.HIGH,
                    onSaving = {},
                    onSuccess = { _, _ ->
                        if (continuation.isActive) continuation.resumeWithException(
                            IllegalStateException("A captura retornou um vídeo público inesperado."))
                    },
                    onError = { reason ->
                        if (continuation.isActive) continuation.resumeWithException(
                            IllegalStateException(reason))
                    },
                    onRenderedFrame = { file ->
                        // Executado em thread de IO pelo ExportEngine, nunca no Compose.
                        val saved = VideoFrameSaver.saveRenderedFrame(
                            context.applicationContext, file, job.targetInVideoMs, fileName)
                        if (continuation.isActive) continuation.resume(saved)
                        else runCatching { context.contentResolver.delete(saved, null, null) }
                    })
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        }
    }
}
