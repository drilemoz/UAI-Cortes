package br.com.uaicortes

import android.content.Context
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Decode in presentation order, not the compressed packet order (B frames are normal). */
internal object ExportFrameSampling {
    data class Sample(val frames: List<ExportFrameOrder.Frame>, val durationUs: Long)

    suspend fun read(context: Context, uri: Uri, startUs: Long, endUs: Long): Sample {
        require(endUs > startUs && endUs - startUs <= 5_100_000L)
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var video = -1
            for (i in 0 until extractor.trackCount) {
                if (extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                    video = i
                    break
                }
            }
            require(video >= 0) { "Sem faixa de vídeo" }
            val format = extractor.getTrackFormat(video)
            val rotation = if (format.containsKey(MediaFormat.KEY_ROTATION)) format.getInteger(MediaFormat.KEY_ROTATION) else 0
            val duration = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            format.setInteger(MediaFormat.KEY_ROTATION, 0)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            extractor.selectTrack(video)
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val decoder = openDecoder(format)
            codec = decoder
            val samples = ArrayList<ExportFrameOrder.Frame>()
            val info = MediaCodec.BufferInfo()
            val deadline = SystemClock.elapsedRealtime() + 25_000L
            var inputDone = false
            val allowanceUs = 200_000L
            while (SystemClock.elapsedRealtime() < deadline) {
                currentCoroutineContext().ensureActive()
                if (!inputDone) {
                    val index = decoder.dequeueInputBuffer(0)
                    if (index >= 0) {
                        val buffer = requireNotNull(decoder.getInputBuffer(index))
                        buffer.clear()
                        val pts = extractor.sampleTime
                        val size = if (pts < 0L) -1 else extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(index, 0, size, pts, 0)
                            extractor.advance()
                        }
                    }
                }
                val index = decoder.dequeueOutputBuffer(info, 7_000L)
                if (index < 0) continue
                val pts = info.presentationTimeUs
                val eos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                try {
                    if (info.size > 0 && pts >= startUs && pts < endUs) {
                        require(samples.size < 640) { "Limite de quadros do diagnóstico" }
                        val image = requireNotNull(decoder.getOutputImage(index)) { "Decodificador sem saída YUV" }
                        val signature = image.use { luma(it, rotation) }
                        samples.add(ExportFrameOrder.Frame(pts, signature))
                    }
                } finally { decoder.releaseOutputBuffer(index, false) }
                if (eos || (info.size > 0 && pts >= endUs + allowanceUs))
                    return Sample(samples.sortedBy { it.timeUs }, duration)
            }
            error("Tempo limite da leitura de quadros")
        } finally {
            try { codec?.release() } finally { extractor.release() }
        }
    }

    /** Prefer an independent software decoder to compare the output of the hardware export. */
    private fun openDecoder(format: MediaFormat): MediaCodec {
        val mime = requireNotNull(format.getString(MediaFormat.KEY_MIME))
        val software = runCatching {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.firstOrNull { info ->
                !info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) } &&
                    (if (Build.VERSION.SDK_INT >= 29) info.isSoftwareOnly
                     else info.name.startsWith("OMX.google.") || info.name.startsWith("c2.android."))
            }?.name
        }.getOrNull()
        fun start(decoder: MediaCodec): MediaCodec = try {
            decoder.configure(format, null, null, 0)
            decoder.start()
            decoder
        } catch (error: Exception) {
            runCatching { decoder.release() }
            throw error
        }
        if (software != null) {
            try {
                return start(MediaCodec.createByCodecName(software))
            } catch (_: Exception) {
                // Some profiles or sizes are only supported by the device decoder.
            }
        }
        return start(MediaCodec.createDecoderByType(mime))
    }

    private fun luma(image: Image, degrees: Int): ByteArray {
        val rect = image.cropRect
        val plane = image.planes.first()
        val buf = plane.buffer
        val base = buf.position()
        val rotation = Math.floorMod(degrees, 360)
        val xPoints = FloatArray(ExportFrameOrder.WIDTH) { .12f + (it + .5f) * .76f / ExportFrameOrder.WIDTH }
        val yPoints = FloatArray(ExportFrameOrder.HEIGHT) { .08f + (it + .5f) * .78f / ExportFrameOrder.HEIGHT }
        fun at(x: Float, y: Float): Int {
            val u = when (rotation) { 90 -> y; 180 -> 1f - x; 270 -> 1f - y; else -> x }
            val v = when (rotation) { 90 -> 1f - x; 180 -> 1f - y; 270 -> x; else -> y }
            val px = rect.left + (u * rect.width()).toInt().coerceIn(0, rect.width() - 1)
            val py = rect.top + (v * rect.height()).toInt().coerceIn(0, rect.height() - 1)
            return buf.get(base + py * plane.rowStride + px * plane.pixelStride).toInt() and 255
        }
        val dx = .76f / ExportFrameOrder.WIDTH / 4f
        val dy = .78f / ExportFrameOrder.HEIGHT / 4f
        return ByteArray(ExportFrameOrder.PIXELS) { index ->
            val x = xPoints[index % ExportFrameOrder.WIDTH]
            val y = yPoints[index / ExportFrameOrder.WIDTH]
            ((at(x - dx, y - dy) + at(x + dx, y - dy) +
                at(x - dx, y + dy) + at(x + dx, y + dy)) / 4).toByte()
        }
    }
}
