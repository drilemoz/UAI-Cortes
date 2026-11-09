package br.com.uaicortes

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.max

/** Extrai os picos em streaming, sem carregar PCM completo na memória. */
internal object AudioClipWaveform {
    data class Inspection(val durationMs: Long, val peaks: List<Float>)

    suspend fun duration(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        MediaExtractor().useSafely { extractor ->
            extractor.setDataSource(context, uri, null)
            val audio = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("O arquivo não contém faixa de áudio.")
            val format = extractor.getTrackFormat(audio)
            val direct = if (format.containsKey(MediaFormat.KEY_DURATION))
                format.getLong(MediaFormat.KEY_DURATION) / 1_000L else 0L
            val duration = direct.takeIf { it > 0L } ?: runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                } finally { retriever.release() }
            }.getOrDefault(0L)
            require(duration in AudioClipSelectionPolicy.MIN_SELECTION_MS..AudioClipSelectionPolicy.MAX_DURATION_MS) {
                "Não foi possível determinar a duração ou ela excede 6 horas."
            }
            duration
        }
    }

    private inline fun <T> MediaExtractor.useSafely(block: (MediaExtractor) -> T): T =
        try { block(this) } finally { release() }

    suspend fun inspect(context: Context, uri: Uri): Inspection = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val index = (0 until extractor.trackCount).firstOrNull { track ->
                extractor.getTrackFormat(track).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("O arquivo não contém uma faixa de áudio reconhecida.")
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: error("Formato sem identificação de áudio.")
            val durationMs = (if (format.containsKey(MediaFormat.KEY_DURATION))
                format.getLong(MediaFormat.KEY_DURATION) / 1_000L else 0L)
                .takeIf { it > 0L } ?: error("Não foi possível determinar a duração do áudio.")
            require(durationMs in AudioClipSelectionPolicy.MIN_SELECTION_MS..AudioClipSelectionPolicy.MAX_DURATION_MS) {
                "O editor aceita arquivos entre 0,1 segundo e 6 horas."
            }
            // Vídeos grandes são editáveis, mas não se deve decodificar horas só para desenhar ondas.
            if (durationMs > 30L * 60L * 1_000L) return@withContext Inspection(durationMs, emptyList())
            extractor.selectTrack(index)
            val peaks = FloatArray(AudioClipSelectionPolicy.WAVEFORM_BARS)
            codec = MediaCodec.createDecoderByType(mime)
            val decoder = codec
            decoder.configure(format, null, null, 0)
            decoder.start()
            var inputDone = false
            var outputDone = false
            val bufferInfo = MediaCodec.BufferInfo()
            val startedAt = SystemClock.elapsedRealtime()
            while (!outputDone) {
                coroutineContext.ensureActive()
                if (SystemClock.elapsedRealtime() - startedAt > 40_000L)
                    error("A análise da onda sonora excedeu o limite de tempo.")
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val input = decoder.getInputBuffer(inputIndex) ?: error("Decodificador sem buffer de entrada")
                        input.clear()
                        val size = extractor.readSampleData(input, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                when {
                    outIndex >= 0 -> {
                        val buffer = decoder.getOutputBuffer(outIndex)
                        if (buffer != null && bufferInfo.size > 0) {
                            val pcmEncoding = runCatching {
                                decoder.outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            }.getOrDefault(AudioFormat.ENCODING_PCM_16BIT)
                            if (pcmEncoding != AudioFormat.ENCODING_PCM_FLOAT &&
                                pcmEncoding != AudioFormat.ENCODING_PCM_16BIT)
                                error("Formato PCM não suportado para visualizar a onda.")
                            val floatPcm = pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT
                            val bytesPerSample = if (floatPcm) 4 else 2
                            // Uma representação simples de picos por quadros decodificados.
                            val out = buffer.duplicate().order(ByteOrder.nativeOrder())
                            val limit = minOf(bufferInfo.offset + bufferInfo.size, out.limit())
                            val sampleCount = (limit - bufferInfo.offset) / bytesPerSample
                            val sampleRate = runCatching { decoder.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) }
                                .getOrDefault(44_100).coerceAtLeast(1)
                            val channels = runCatching { decoder.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }
                                .getOrDefault(1).coerceAtLeast(1)
                            val step = max(1, sampleCount / 900)
                            for (sample in 0 until sampleCount step step) {
                                val position = bufferInfo.offset + sample * bytesPerSample
                                val amplitude = if (floatPcm) abs(out.getFloat(position)).coerceIn(0f, 1f)
                                else abs(out.getShort(position).toInt() / 32768f).coerceIn(0f, 1f)
                                val timeUs = bufferInfo.presentationTimeUs +
                                    sample.toLong() * 1_000_000L / (sampleRate.toLong() * channels)
                                val bar = ((timeUs * peaks.size) / (durationMs * 1_000L))
                                    .toInt().coerceIn(0, peaks.lastIndex)
                                peaks[bar] = max(peaks[bar], amplitude)
                            }
                        }
                        decoder.releaseOutputBuffer(outIndex, false)
                        outputDone = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    }
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                }
            }
            // Retorno contendo apenas 160 amplitudes, independentemente do tamanho da fonte.
            Inspection(durationMs, peaks.map { it.coerceIn(0.02f, 1f) })
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }
}
