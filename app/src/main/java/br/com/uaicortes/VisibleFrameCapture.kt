package br.com.uaicortes

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Copia somente a área da prévia renderizada, incluindo PiP, textos e efeitos. */
internal object VisibleFrameCapture {
    fun findActivity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }

    fun validBounds(rect: Rect, width: Int, height: Int): Boolean =
        rect.left >= 0 && rect.top >= 0 && rect.right <= width && rect.bottom <= height &&
            rect.width() > 1 && rect.height() > 1

    suspend fun copy(activity: Activity, source: Rect): Bitmap {
        val decor = activity.window.decorView
        require(validBounds(source, decor.width, decor.height)) {
            "A prévia ainda não está totalmente visível. Posicione o vídeo e tente novamente."
        }
        return suspendCancellableCoroutine { continuation ->
            val bitmap = Bitmap.createBitmap(source.width(), source.height(), Bitmap.Config.ARGB_8888)
            try {
                PixelCopy.request(activity.window, source, bitmap, { result ->
                    if (result == PixelCopy.SUCCESS) {
                        if (continuation.isActive) continuation.resume(bitmap) else bitmap.recycle()
                    } else {
                        bitmap.recycle()
                        if (continuation.isActive) continuation.resumeWithException(
                            IllegalStateException("Falha ao copiar a prévia (PixelCopy: $result). Tente novamente."))
                    }
                }, Handler(Looper.getMainLooper()))
            } catch (e: Exception) {
                bitmap.recycle()
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        }
    }
}
