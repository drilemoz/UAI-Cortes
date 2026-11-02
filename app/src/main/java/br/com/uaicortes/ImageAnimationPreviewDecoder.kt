package br.com.uaicortes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

internal data class ImagePreviewDecoded(
    val bitmap: Bitmap,
    val originalWidth: Int,
    val originalHeight: Int
)

/** Decodifica a imagem real via SAF/MediaStore, preserva proporção e orientação EXIF.
 *  Evita loadThumbnail(): alguns providers devolvem miniatura quadrada e/ou de baixa qualidade.
 *  O resultado é limitado a 1920 px no maior lado para reduzir pressão de memória no Android.
 */
internal object ImageAnimationPreviewDecoder {
    fun decode(context: Context, uri: Uri, quality: ImagePreviewQuality): ImagePreviewDecoded? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Imagem indisponível" }
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val swap = orientation in setOf(ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE)
        val originalWidth = if (swap) bounds.outHeight else bounds.outWidth
        val originalHeight = if (swap) bounds.outWidth else bounds.outHeight
        val options = BitmapFactory.Options().apply {
            inSampleSize = ImagePreviewSizing.sampleSize(bounds.outWidth, bounds.outHeight, quality.maxLongSide)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Não foi possível ler a foto")
        val transformed = try {
            val matrix = Matrix()
            val flip = orientation in setOf(ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
                ExifInterface.ORIENTATION_FLIP_VERTICAL, ExifInterface.ORIENTATION_TRANSPOSE,
                ExifInterface.ORIENTATION_TRANSVERSE)
            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90f
                ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180f
                ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270f
                else -> 0f
            }
            if (flip) matrix.postScale(-1f, 1f)
            if (rotation != 0f) matrix.postRotate(rotation)
            if (flip || rotation != 0f) Bitmap.createBitmap(decoded, 0, 0,
                decoded.width, decoded.height, matrix, true) else decoded
        } finally { /* Bitmap usada pelo Compose não deve ser reciclada prematuramente. */ }
        if (transformed !== decoded) decoded.recycle()
        val (targetW, targetH) = ImagePreviewSizing.target(transformed.width,
            transformed.height, quality.maxLongSide)
        val finalBitmap = if (targetW != transformed.width || targetH != transformed.height) {
            Bitmap.createScaledBitmap(transformed, targetW, targetH, true).also { transformed.recycle() }
        } else transformed
        ImagePreviewDecoded(finalBitmap, originalWidth, originalHeight)
    }.getOrNull()
}
