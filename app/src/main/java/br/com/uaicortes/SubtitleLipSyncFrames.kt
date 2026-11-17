package br.com.uaicortes

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

internal data class SubtitleLipFrames(val task: SubtitleLipSyncPolicy.Task,
    val start: List<ByteArray>?, val end: List<ByteArray>?)

/** Read close frames from the trimmed video source, with no audio and no temporary media files. */
internal object SubtitleLipSyncFrames {
    private const val LONG_SIDE = 432
    private const val MAX_JPEG = 60 * 1024

    suspend fun extract(context: Context, tasks: List<SubtitleLipSyncPolicy.Task>,
        progress: suspend (Int, Int) -> Unit): List<SubtitleLipFrames> = withContext(Dispatchers.IO) {
        val cancel = currentCoroutineContext()
        val cache = mutableMapOf<Pair<String, Long>, ByteArray?>()
        val sources = mutableMapOf<String, Source?>()
        try {
            fun load(uri: String) = sources.getOrPut(uri) { openSource(context, uri) }
            fun frames(window: SubtitleLipSyncPolicy.Window?): List<ByteArray>? {
                if (window == null) return null
                val source = load(window.uri) ?: return null
                val images = window.sourceMs.map { ms ->
                    cancel.ensureActive()
                    cache.getOrPut(window.uri to ms) { frame(source, ms) } ?: return null
                }
                // A duplicated/still sequence is not sufficient visual evidence.
                if (images.zipWithNext().count { (a, b) -> !a.contentEquals(b) } < 3) return null
                return images
            }
            val result = mutableListOf<SubtitleLipFrames>()
            for ((i, task) in tasks.withIndex()) {
                progress(i + 1, tasks.size)
                val start = frames(task.start)
                val end = frames(task.end)
                if (start != null || end != null) result += SubtitleLipFrames(
                    task.copy(start = task.start.takeIf { start != null }, end = task.end.takeIf { end != null }),
                    start, end)
            }
            result
        } finally {
            sources.values.forEach { source -> if (source != null) runCatching { source.retriever.release() } }
        }
    }

    private data class Source(val retriever: MediaMetadataRetriever, val width: Int, val height: Int)

    private fun openSource(context: Context, uri: String): Source? {
        val reader = MediaMetadataRetriever()
        return try {
            reader.setDataSource(context, Uri.parse(uri))
            var width = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            var height = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
            if (rotation == 90 || rotation == 270) { val tmp = width; width = height; height = tmp }
            require(width > 0 && height > 0)
            val scale = minOf(1.0, LONG_SIDE.toDouble() / maxOf(width, height))
            Source(reader, (width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
        } catch (_: Exception) {
            runCatching { reader.release() }
            null
        }
    }

    private fun frame(source: Source, timeMs: Long): ByteArray? {
        val reader = source.retriever
        var raw: Bitmap? = null
        var limited: Bitmap? = null
        return try {
            val timeUs = timeMs * 1000
            val image = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
                reader.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST, source.width, source.height)
            else reader.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)) ?: return null
            raw = image
            val side = maxOf(image.width, image.height)
            val scaled = if (side <= LONG_SIDE) image else Bitmap.createScaledBitmap(image,
                (image.width * LONG_SIDE / side).coerceAtLeast(1),
                (image.height * LONG_SIDE / side).coerceAtLeast(1), true)
            limited = scaled
            val out = ByteArrayOutputStream()
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, 76, out)) return null
            if (out.size() > MAX_JPEG) {
                out.reset()
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, 55, out)) return null
            }
            if (out.size() !in 1..MAX_JPEG) null else out.toByteArray()
        } catch (_: Exception) {
            null
        } finally {
            if (limited !== raw) limited?.recycle()
            raw?.recycle()
        }
    }
}
