package br.com.uaicortes

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.sqrt

/** Decodifica apenas a vizinhança da fala selecionada. Nunca transmite ou persiste o áudio. */
internal object ReferenceAudioProbe {
    data class Slice(
        val uri: Uri,
        val sourceFromMs: Long,
        val sourceUntilMs: Long,
        val projectOffsetMs: Long
    )

    fun projectSlices(project: VideoProject, window: ReferenceAudioActivity.Window): List<Slice> {
        val list = mutableListOf<Slice>()
        var projectStart = 0L
        for (clip in project.clips) {
            val duration = (clip.endMs - clip.startMs).coerceAtLeast(0L)
            val overlapStart = maxOf(window.fromMs, projectStart)
            val overlapEnd = minOf(window.untilMs, projectStart + duration)
            if (overlapStart < overlapEnd) list += Slice(
                Uri.parse(clip.uri), clip.startMs + overlapStart - projectStart,
                clip.startMs + overlapEnd - projectStart, projectStart - clip.startMs
            )
            projectStart += duration
        }
        return list
    }

    fun trackSlices(track: AudioTrack, window: ReferenceAudioActivity.Window): List<Slice> {
        val overlapStart = maxOf(window.fromMs, track.startMs)
        val overlapEnd = minOf(window.untilMs, track.startMs + track.durationMs)
        return if (overlapStart >= overlapEnd) emptyList() else listOf(Slice(
            Uri.parse(track.uri), track.sourceStartMs + overlapStart - track.startMs,
            track.sourceStartMs + overlapEnd - track.startMs, track.startMs - track.sourceStartMs
        ))
    }

    fun externalSlices(uri: Uri, offsetMs: Long, durationMs: Long,
        window: ReferenceAudioActivity.Window): List<Slice> {
        val overlapStart = maxOf(window.fromMs, offsetMs)
        val overlapEnd = minOf(window.untilMs, offsetMs + durationMs)
        return if (overlapStart >= overlapEnd) emptyList() else listOf(Slice(
            uri, overlapStart - offsetMs, overlapEnd - offsetMs, offsetMs
        ))
    }

    suspend fun analyze(context: Context, slices: List<Slice>,
        window: ReferenceAudioActivity.Window, cueStartMs: Long): ReferenceAudioActivity.Result =
        withContext(Dispatchers.IO) {
            if (slices.isEmpty()) throw IllegalArgumentException("Não há áudio nesta região do projeto.")
            if (slices.size > 16) throw IllegalArgumentException(
                "A região contém mais de 16 cortes. Selecione uma referência de áudio contínua.")
            val deadline = SystemClock.elapsedRealtime() + 20_000L
            val sums = DoubleArray(ReferenceAudioActivity.BINS)
            val counts = IntArray(ReferenceAudioActivity.BINS)
            for (slice in slices) {
                currentCoroutineContext().ensureActive()
                decodeSlice(context, slice, window, sums, counts, deadline)
            }
            if (counts.all { it == 0 }) throw IllegalArgumentException(
                "Não foi possível decodificar o áudio desta região. Confira a origem selecionada.")
            val levels = sums.indices.map { i ->
                if (counts[i] == 0) 0f else sqrt(sums[i] / counts[i]).toFloat().coerceIn(0f, 1f)
            }
            ReferenceAudioActivity.detect(levels, window.fromMs, window.untilMs, cueStartMs)
        }

    private suspend fun decodeSlice(context: Context, slice: Slice,
        window: ReferenceAudioActivity.Window, sums: DoubleArray, counts: IntArray,
        deadline: Long) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(context, slice.uri, null)
            val index = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return
            extractor.selectTrack(index)
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()
            val startUs = slice.sourceFromMs * 1000L
            val stopUs = slice.sourceUntilMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            var sentEos = false
            var gotEos = false
            var iterations = 0
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(1)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            val info = MediaCodec.BufferInfo()
            while (!gotEos && iterations++ < 15_000) {
                currentCoroutineContext().ensureActive()
                if (SystemClock.elapsedRealtime() > deadline)
                    throw IllegalStateException("A análise excedeu o limite de tempo do aparelho.")
                if (!sentEos) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val input = decoder.getInputBuffer(inputIndex)
                        val sampleTime = extractor.sampleTime
                        if (input == null || sampleTime < 0 || sampleTime > stopUs + 250_000L) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sentEos = true
                        } else {
                            input.clear()
                            val size = extractor.readSampleData(input, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                sentEos = true
                            } else {
                                decoder.queueInputBuffer(inputIndex, 0, size, sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }
                val outputIndex = decoder.dequeueOutputBuffer(info, 10_000)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = decoder.outputFormat
                        if (outputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE))
                            sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(1)
                        if (outputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                            channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                        if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING))
                            encoding = outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    }
                    outputIndex >= 0 -> {
                        val output = decoder.getOutputBuffer(outputIndex)
                        if (output != null && info.size > 0) {
                            output.position(info.offset)
                            output.limit(info.offset + info.size)
                            addPcm(output.slice().order(ByteOrder.nativeOrder()), encoding,
                                channels, sampleRate, info.presentationTimeUs,
                                slice, window, sums, counts)
                        }
                        gotEos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
            if (iterations >= 15_000) throw IllegalStateException("A decodificação ultrapassou o limite seguro.")
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            extractor.release()
        }
    }

    private fun addPcm(buffer: ByteBuffer, encoding: Int, channels: Int, sampleRate: Int,
        ptsUs: Long, slice: Slice, window: ReferenceAudioActivity.Window,
        sums: DoubleArray, counts: IntArray) {
        val floatPcm = encoding == AudioFormat.ENCODING_PCM_FLOAT
        val bytesPerSample = if (floatPcm) 4 else 2
        if (!floatPcm && encoding != AudioFormat.ENCODING_PCM_16BIT) return
        val frameBytes = channels * bytesPerSample
        val frameCount = buffer.remaining() / frameBytes
        if (frameCount <= 0) return
        val step = max(1, sampleRate / 4000)
        for (frame in 0 until frameCount step step) {
            val timeMs = (ptsUs + frame.toLong() * 1_000_000L / sampleRate) / 1000L
            if (timeMs < slice.sourceFromMs || timeMs >= slice.sourceUntilMs) continue
            val projectMs = timeMs + slice.projectOffsetMs
            if (projectMs < window.fromMs || projectMs >= window.untilMs) continue
            val bin = ((projectMs - window.fromMs) * sums.size /
                (window.untilMs - window.fromMs).coerceAtLeast(1L)).toInt().coerceIn(0, sums.lastIndex)
            var square = 0.0
            for (channel in 0 until channels) {
                val byteIndex = frame * frameBytes + channel * bytesPerSample
                val sample = if (floatPcm) buffer.getFloat(byteIndex).toDouble()
                    else buffer.getShort(byteIndex).toDouble() / 32768.0
                square += sample * sample
            }
            sums[bin] += square / channels
            counts[bin]++
        }
    }
}
