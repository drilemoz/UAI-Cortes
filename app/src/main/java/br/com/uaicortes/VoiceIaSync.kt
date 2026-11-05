package br.com.uaicortes

import java.io.IOException
import java.util.Locale
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt

/** Sincronização assistida: não altera legendas, IDs ou posição de outras faixas. */
internal object VoiceIaSync {
    const val MAX_LOCAL_SPEED = 1.25
    const val AUTO_SUGGEST_SPEED = 1.10

    data class Review(val availableMs: Long, val excessMs: Long, val requiredSpeed: Double,
        val nextOverlapMs: Long, val canStretch: Boolean, val mild: Boolean)

    fun review(startMs: Long, cueEndMs: Long, nextCueStartMs: Long?,
        projectEndMs: Long, audioDurationMs: Long): Review {
        val limit = minOf(cueEndMs, nextCueStartMs ?: Long.MAX_VALUE, projectEndMs)
        val available = (limit - startMs).coerceAtLeast(0L)
        val excess = (audioDurationMs - available).coerceAtLeast(0L)
        val rate = if (available > 0) audioDurationMs.toDouble() / available else Double.POSITIVE_INFINITY
        val overlaps = if (nextCueStartMs == null) 0L else
            (startMs + audioDurationMs - nextCueStartMs).coerceAtLeast(0L)
        return Review(available, excess, rate, overlaps,
            excess > 0 && available >= 250L && rate <= MAX_LOCAL_SPEED,
            excess > 0 && rate <= AUTO_SUGGEST_SPEED)
    }

    /** Instrução apenas orientativa: a API não promete duração precisa. */
    fun guidance(style: String, targetMs: Long, brisk: Boolean = false): String {
        val seconds = String.format(Locale.ROOT, "%.1f", targetMs.coerceAtLeast(250L) / 1000.0)
        return style + if (brisk)
            ", speak briskly without long pauses, aim for approximately $seconds seconds, " +
                "speak every original word, never omit or rewrite the text"
        else
            ", aim for approximately $seconds seconds with natural pacing and short pauses, " +
                "speak every original word, never omit or rewrite the text"
    }
}

/**
 * Ajuste offline por overlap-add de janelas PCM 16 bits mono; não altera frequência de amostragem.
 * WAV original é preservado. Restringe tamanho/duração e fator para evitar excesso de memória
 * e alterações grosseiras. A qualidade auditiva deve ser confirmada pelo usuário antes de inserir.
 */
internal object VoiceIaWavStretch {
    private fun u16(data: ByteArray, p: Int): Int =
        (data[p].toInt() and 255) or ((data[p + 1].toInt() and 255) shl 8)
    private fun u32(data: ByteArray, p: Int): Int = u16(data, p) or (u16(data, p + 2) shl 16)
    private fun write16(data: ByteArray, p: Int, n: Int) {
        data[p] = n.toByte(); data[p + 1] = (n ushr 8).toByte()
    }
    private fun write32(data: ByteArray, p: Int, n: Int) {
        write16(data, p, n); write16(data, p + 2, n ushr 16)
    }
    fun shorten(wav: ByteArray, targetMs: Long): GeminiWave {
        require(wav.size <= 4 * 1024 * 1024) { "WAV muito grande para ajuste local. Regere a voz." }
        val sourceMs = GeminiWaveParser.duration(wav, "audio/wav")
        val ratio = sourceMs.toDouble() / targetMs.coerceAtLeast(1L)
        require(ratio > 1.0001 && ratio <= VoiceIaSync.MAX_LOCAL_SPEED) {
            "Diferença grande para ajuste local. Regere com ritmo mais rápido." }
        var at = 12
        var rate = 0
        var dataStart = -1
        var dataSize = 0
        while (at + 8 <= wav.size) {
            val size = u32(wav, at + 4).toLong() and 0xffffffffL
            val beginning = at + 8
            require(size <= wav.size - beginning) { "Bloco WAV inválido." }
            when (String(wav, at, 4, Charsets.US_ASCII)) {
                "fmt " -> { rate = u32(wav, beginning + 4) }
                "data" -> { dataStart = beginning; dataSize = size.toInt() }
            }
            at = (beginning + size + (size and 1L)).toInt()
        }
        require(rate in 8000..96000 && dataStart >= 0 && dataSize >= 1024 && dataSize % 2 == 0)
        val inputCount = dataSize / 2
        val outputCount = (targetMs.toDouble() * rate / 1000.0).roundToInt()
        require(outputCount >= 250 * rate / 1000 && outputCount < inputCount)
        require(sourceMs <= 25000L) { "Áudio longo demais para ajuste local." }
        val input = ShortArray(inputCount) { idx -> u16(wav, dataStart + idx * 2).toShort() }
        val sums = FloatArray(outputCount)
        val weights = FloatArray(outputCount)
        val window = (rate / 25).coerceIn(320, 3840)  // cerca de 40 ms
        val hop = (window / 4).coerceAtLeast(80)
        val taper = FloatArray(window) { i ->
            (0.5 - 0.5 * cos(2 * PI * (i + .5) / window)).toFloat()
        }
        var outStart = 0
        while (outStart < outputCount) {
            val srcStart = (outStart * ratio).roundToInt()
            if (srcStart >= inputCount) break
            val n = minOf(window, outputCount - outStart, inputCount - srcStart)
            for (i in 0 until n) {
                val pos = outStart + i
                val weight = taper[i]
                sums[pos] += input[srcStart + i] * weight
                weights[pos] += weight
            }
            outStart += hop
        }
        // Ponta final: manter a última amostra disponível quando a janela final for incompleta.
        val bytes = ByteArray(44 + outputCount * 2)
        "RIFF".toByteArray().copyInto(bytes, 0)
        write32(bytes, 4, bytes.size - 8)
        "WAVEfmt ".toByteArray().copyInto(bytes, 8)
        write32(bytes, 16, 16); write16(bytes, 20, 1); write16(bytes, 22, 1)
        write32(bytes, 24, rate); write32(bytes, 28, rate * 2)
        write16(bytes, 32, 2); write16(bytes, 34, 16)
        "data".toByteArray().copyInto(bytes, 36)
        write32(bytes, 40, outputCount * 2)
        for (i in 0 until outputCount) {
            val sample = if (weights[i] > .00001f) (sums[i] / weights[i]).roundToInt()
                else input[minOf((i * ratio).roundToInt(), input.lastIndex)].toInt()
            write16(bytes, 44 + 2 * i, sample.coerceIn(-32768, 32767))
        }
        return GeminiWave(bytes, GeminiWaveParser.duration(bytes, "audio/wav"))
    }
}
