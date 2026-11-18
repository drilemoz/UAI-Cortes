package br.com.uaicortes

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Decodificação local e limitada. Falhas são tratadas pelo importador sem descartar o original. */
internal object ImportedVoicePcm {
    private const val MAX_PCM = 4 * 1024 * 1024 - 44

    suspend fun read(file: File, durationMs: Long): GeminiWave? {
        if (durationMs !in 250L..25_000L) return null
        if (file.extension.equals("wav", ignoreCase = true) && file.length() in 45L..(MAX_PCM + 44L)) {
            val bytes = file.readBytes()
            val measured = runCatching { GeminiWaveParser.duration(bytes, "audio/wav") }.getOrNull()
            if (measured != null) return GeminiWave(bytes, measured)
        }
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val job = currentCoroutineContext()
        try {
            extractor.setDataSource(file.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val type = format.getString(MediaFormat.KEY_MIME) ?: return null
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING))
                format.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
            val sink = ByteArrayOutputStream()
            val deadline = SystemClock.elapsedRealtime() + 8_000L
            fun append(raw: ByteBuffer) {
                check(rate in 8_000..96_000 && channels in 1..8)
                val bytes = when (encoding) {
                    AudioFormat.ENCODING_PCM_16BIT -> 2
                    AudioFormat.ENCODING_PCM_FLOAT -> 4
                    else -> error("PCM não suportado no ajuste")
                }
                raw.order(ByteOrder.nativeOrder())
                val frames = raw.remaining() / (channels * bytes)
                check(frames <= (MAX_PCM - sink.size()) / 2)
                repeat(frames) {
                    var sum = 0
                    repeat(channels) {
                        sum += if (bytes == 2) raw.short.toInt()
                            else (raw.float.coerceIn(-1f, 1f) * 32767).toInt()
                    }
                    val sample = sum / channels
                    sink.write(sample and 255)
                    sink.write((sample shr 8) and 255)
                }
            }
            if (type == "audio/raw") {
                val sample = ByteBuffer.allocateDirect(256 * 1024)
                while (true) {
                    job.ensureActive()
                    check(SystemClock.elapsedRealtime() <= deadline)
                    sample.clear()
                    val size = extractor.readSampleData(sample, 0)
                    if (size < 0) break
                    sample.position(0)
                    sample.limit(size)
                    append(sample)
                    if (!extractor.advance()) break
                }
            } else {
                val decoder = MediaCodec.createDecoderByType(type)
                codec = decoder
                decoder.configure(format, null, null, 0)
                decoder.start()
                val info = MediaCodec.BufferInfo()
                var inputEnded = false
                var outputEnded = false
                while (!outputEnded) {
                    job.ensureActive()
                    check(SystemClock.elapsedRealtime() <= deadline)
                    if (!inputEnded) {
                        val index = decoder.dequeueInputBuffer(5_000L)
                        if (index >= 0) {
                            val buffer = checkNotNull(decoder.getInputBuffer(index))
                            buffer.clear()
                            val size = extractor.readSampleData(buffer, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEnded = true
                            } else {
                                decoder.queueInputBuffer(index, 0, size, extractor.sampleTime.coerceAtLeast(0L), 0)
                                extractor.advance()
                            }
                        }
                    }
                    val index = decoder.dequeueOutputBuffer(info, 5_000L)
                    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val output = decoder.outputFormat
                        val newRate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        check(sink.size() == 0 || rate == newRate)
                        rate = newRate
                        channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        encoding = if (output.containsKey(MediaFormat.KEY_PCM_ENCODING))
                            output.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                    } else if (index >= 0) {
                        if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            val buffer = checkNotNull(decoder.getOutputBuffer(index))
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            append(buffer.slice())
                        }
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(index, false)
                    }
                }
            }
            val pcm = sink.toByteArray()
            if (pcm.isEmpty() || rate !in 8_000..96_000) return null
            val wav = ByteBuffer.allocate(44 + pcm.size).order(ByteOrder.LITTLE_ENDIAN)
            wav.put("RIFF".toByteArray(Charsets.US_ASCII)).putInt(36 + pcm.size)
                .put("WAVEfmt ".toByteArray(Charsets.US_ASCII)).putInt(16)
                .putShort(1).putShort(1).putInt(rate).putInt(rate * 2)
                .putShort(2).putShort(16).put("data".toByteArray(Charsets.US_ASCII))
                .putInt(pcm.size).put(pcm)
            return GeminiWave(wav.array(), GeminiWaveParser.duration(wav.array(), "audio/wav"))
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }
}
