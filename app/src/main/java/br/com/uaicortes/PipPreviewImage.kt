package br.com.uaicortes

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import br.com.uaicortes.EditorDiagnosticPolicy.MediaStage
import br.com.uaicortes.EditorDiagnosticPolicy.Result
import br.com.uaicortes.EditorDiagnosticPolicy.Target
import java.io.FileNotFoundException
import java.io.IOException

/** Decode only a preview-sized image. Do not retain the URI in diagnostics. */
internal fun decodePipPreviewImage(resolver: ContentResolver, uri: Uri): Bitmap? {
    var stage = MediaStage.PREVIEW_OPEN
    fun failed(result: Result): Bitmap? {
        DiagnosticRecorder.editorMedia(Target.PIP_IMAGE, stage, result)
        return null
    }
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val header = resolver.openInputStream(uri) ?: return failed(Result.NOT_FOUND)
        stage = MediaStage.PREVIEW_DECODE
        header.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) return failed(Result.INVALID_IMAGE)
        var sample = 1
        while (options.outWidth / sample > 1400 || options.outHeight / sample > 1400) sample *= 2
        options.inJustDecodeBounds = false
        options.inSampleSize = sample
        stage = MediaStage.PREVIEW_OPEN
        val body = resolver.openInputStream(uri) ?: return failed(Result.NOT_FOUND)
        stage = MediaStage.PREVIEW_DECODE
        body.use { BitmapFactory.decodeStream(it, null, options) } ?: failed(Result.INVALID_IMAGE)
    } catch (_: SecurityException) {
        failed(Result.PERMISSION)
    } catch (_: FileNotFoundException) {
        failed(Result.NOT_FOUND)
    } catch (_: IOException) {
        failed(Result.FAILED)
    } catch (_: RuntimeException) {
        failed(Result.FAILED)
    }
}
