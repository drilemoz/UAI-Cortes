package br.com.uaicortes

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

/** Inspeção de contêiner/streams, NÃO escuta nem inspeciona pixels de mídia. */
internal object ExportMp4Inspector {
    data class Streams(val video: Boolean, val audio: Boolean, val durationMs: Long)

    private fun inspect(context: Context, uri: Uri): Streams {
        val extractor = MediaExtractor()
        val retriever = MediaMetadataRetriever()
        try {
            extractor.setDataSource(context, uri, null)
            var video = false
            var audio = false
            repeat(extractor.trackCount) { index ->
                val mime = extractor.getTrackFormat(index)
                    .getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/")) video = true
                if (mime.startsWith("audio/")) audio = true
            }
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            return Streams(video, audio, duration)
        } finally {
            extractor.release()
            retriever.release()
        }
    }

    /** Consulta trilhas sem supor que todo MP4 com imagem também possua som. */
    fun sourceHasAudio(context: Context, uri: Uri): Boolean {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, uri, null)
            (0 until extractor.trackCount).any { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            }
        } finally { extractor.release() }
    }

    fun inspectFile(context: Context, file: File): Streams = inspect(context, Uri.fromFile(file))
    fun inspectSaved(context: Context, uri: Uri): Streams = inspect(context, uri)

    fun requirePlayable(streams: Streams, expectedAudio: Boolean) {
        check(streams.video) { "MP4 sem faixa de vídeo. O arquivo não foi publicado." }
        check(streams.durationMs > 0L) { "MP4 sem duração verificável. O arquivo não foi publicado." }
        if (expectedAudio) check(streams.audio) {
            "MP4 sem faixa de áudio, apesar da narração/música configurada. O arquivo não foi publicado."
        }
    }
}
