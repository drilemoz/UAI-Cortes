package br.com.uaicortes

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

/** Consulta opcional do FPS nominal sem decodificar quadros, fora da thread UI. */
internal object VideoFrameRate {
    fun read(context: Context, mediaUri: String): Float? = runCatching {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, Uri.parse(mediaUri), null)
            (0 until extractor.trackCount).firstNotNullOfOrNull { index ->
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (!mime.startsWith("video/") || !format.containsKey(MediaFormat.KEY_FRAME_RATE)) null
                else runCatching { format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat() }
                    .getOrNull()?.takeIf { it.isFinite() && it in 1f..240f }
            }
        } finally { extractor.release() }
    }.getOrNull()
}
