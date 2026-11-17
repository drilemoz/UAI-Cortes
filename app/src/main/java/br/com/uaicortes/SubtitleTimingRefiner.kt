package br.com.uaicortes

import java.io.BufferedInputStream
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

/** Energia por quadro de 10 ms do WAV 16 kHz mono PCM16 usado na transcrição. */
internal class SubtitleVoiceEnvelope(val levels: IntArray) {
    companion object {
        const val FRAME_MS = 10
        private const val FRAME_SAMPLES = 160
        private const val HEADER = 44L

        fun read(wav: File): SubtitleVoiceEnvelope {
            val result = ArrayList<Int>()
            BufferedInputStream(wav.inputStream(), 64 * 1024).use { input ->
                var skipped = 0L
                while (skipped < HEADER) {
                    val n = input.skip(HEADER - skipped)
                    if (n <= 0L) break
                    skipped += n
                }
                val bytes = ByteArray(FRAME_SAMPLES * 2)
                while (true) {
                    var filled = 0
                    while (filled < bytes.size) {
                        val n = input.read(bytes, filled, bytes.size - filled)
                        if (n < 0) break
                        filled += n
                    }
                    if (filled < bytes.size) break
                    var sum = 0.0
                    for (i in 0 until FRAME_SAMPLES) {
                        val v = ((bytes[2 * i].toInt() and 0xFF) or (bytes[2 * i + 1].toInt() shl 8)).toShort().toInt()
                        sum += v.toDouble() * v.toDouble()
                    }
                    result += sqrt(sum / FRAME_SAMPLES).toInt()
                }
            }
            return SubtitleVoiceEnvelope(result.toIntArray())
        }
    }
}

/**
 * Confere os horários devolvidos pelo Gemini com a energia real do áudio enviado.
 * 1) estima um deslocamento global constante (legendas sempre adiantadas/atrasadas);
 * 2) ajusta o início/fim de cada legenda ao começo/fim de fala mais próximo.
 * Nunca altera textos, nunca reordena, e não mexe em nada quando o áudio é ambíguo.
 */
internal object SubtitleTimingRefiner {
    const val MAX_GLOBAL_SHIFT_MS = 600
    const val SNAP_WINDOW_MS = 250
    const val MIN_CUE_MS = 200L
    const val WORD_WINDOW_MS = 120
    private const val MIN_WORD_MS = 30L
    private const val FRAME = SubtitleVoiceEnvelope.FRAME_MS

    data class Result(val cues: List<SubtitleCue>, val globalShiftMs: Long, val snappedCount: Int,
        val wordsSnapped: Int = 0)

    /** Fala = nível acima de um piso adaptativo. Vazio quando o áudio é praticamente silencioso. */
    fun activity(levels: IntArray): BooleanArray {
        if (levels.size < 20) return BooleanArray(levels.size)
        val sorted = levels.sortedArray()
        val floor = sorted[sorted.size / 10]
        val peak = sorted[(sorted.size * 95) / 100]
        if (peak < 200) return BooleanArray(levels.size)
        val threshold = maxOf(floor * 2.5, peak * 0.08, 60.0)
        val raw = BooleanArray(levels.size) { levels[it] > threshold }
        // Fecha lacunas de até 3 quadros (30 ms) dentro da fala.
        var i = 0
        while (i < raw.size) {
            if (!raw[i]) {
                var j = i
                while (j < raw.size && !raw[j]) j++
                if (i > 0 && j < raw.size && j - i <= 3) for (k in i until j) raw[k] = true
                i = j
            } else i++
        }
        return raw
    }

    private fun expectedSpeech(cues: List<SubtitleCue>, frames: Int): BooleanArray {
        val out = BooleanArray(frames)
        for (cue in cues) {
            val spans = if (cue.wordTimings.isNotEmpty()) cue.wordTimings.map { it.startMs to it.endMs }
                else listOf(cue.startMs to cue.endMs)
            for ((a, b) in spans) {
                val from = (a / FRAME).toInt().coerceIn(0, frames)
                val to = ((b + FRAME - 1) / FRAME).toInt().coerceIn(0, frames)
                for (f in from until to) out[f] = true
            }
        }
        return out
    }

    /** Deslocamento (ms) a somar às legendas; 0 quando não há ganho claro. */
    fun estimateShift(cues: List<SubtitleCue>, active: BooleanArray): Long {
        val n = active.size
        if (n < 100 || cues.isEmpty()) return 0L
        val expected = expectedSpeech(cues, n)
        val maxLag = MAX_GLOBAL_SHIFT_MS / FRAME
        fun agreement(lag: Int): Int {
            var count = 0
            for (i in 0 until n) {
                val j = i + lag
                if (j < 0 || j >= n) continue
                if (expected[i] == active[j]) count++
            }
            return count
        }
        val base = agreement(0)
        var bestLag = 0
        var best = base
        for (lag in -maxLag..maxLag) {
            if (lag == 0) continue
            val score = agreement(lag)
            if (score > best || (score == best && bestLag != 0 && abs(lag) < abs(bestLag))) {
                best = score; bestLag = lag
            }
        }
        // Exige ganho de pelo menos 1,5 % dos quadros para evitar ajuste por ruído.
        return if (bestLag != 0 && best - base >= n * 15 / 1000) bestLag.toLong() * FRAME else 0L
    }

    private fun shift(cue: SubtitleCue, delta: Long, total: Long): SubtitleCue {
        if (delta == 0L) return cue
        val start = (cue.startMs + delta).coerceIn(0L, total - 1)
        val end = (cue.endMs + delta).coerceIn(start + 1, total)
        val timings = cue.wordTimings.map {
            val s = (it.startMs + delta).coerceIn(start, end - 1)
            SubtitleWordTiming(s, (it.endMs + delta).coerceIn(s + 1, end))
        }
        return cue.copy(startMs = start, endMs = end, wordTimings = timings)
    }

    private fun retime(cue: SubtitleCue, start: Long, end: Long): SubtitleCue {
        val oldLen = (cue.endMs - cue.startMs).coerceAtLeast(1L).toDouble()
        val newLen = (end - start).toDouble()
        val timings = cue.wordTimings.map {
            val s = (start + ((it.startMs - cue.startMs) * newLen / oldLen)).toLong().coerceIn(start, end - 1)
            val e = (start + ((it.endMs - cue.startMs) * newLen / oldLen)).toLong().coerceIn(s + 1, end)
            SubtitleWordTiming(s, e)
        }
        return cue.copy(startMs = start, endMs = end, wordTimings = timings)
    }

    /** Início de fala (silêncio→fala) mais próximo de [targetMs], ou null. */
    fun nearestOnset(active: BooleanArray, targetMs: Long): Long? {
        val center = (targetMs / FRAME).toInt()
        val window = SNAP_WINDOW_MS / FRAME
        var best: Int? = null
        for (f in (center - window).coerceAtLeast(4)..(center + window).coerceAtMost(active.size - 6)) {
            if (!active[f]) continue
            if ((1..4).any { active[f - it] }) continue
            if ((1..4).any { !active[f + it] }) continue
            if (best == null || abs(f - center) < abs(best - center)) best = f
        }
        return best?.let { it.toLong() * FRAME }
    }

    /** Fim de fala (fala→silêncio) mais próximo de [targetMs], ou null. */
    fun nearestOffset(active: BooleanArray, targetMs: Long): Long? {
        val center = (targetMs / FRAME).toInt()
        val window = SNAP_WINDOW_MS / FRAME
        var best: Int? = null
        for (f in (center - window).coerceAtLeast(5)..(center + window).coerceAtMost(active.size - 5)) {
            if (active[f]) continue
            if (!active[f - 1]) continue
            if ((1..4).any { !active[f - it] }) continue
            if ((0..3).any { active[f + it] }) continue
            if (best == null || abs(f - center) < abs(best - center)) best = f
        }
        return best?.let { it.toLong() * FRAME }
    }

    /** Borda de fala (onset=silêncio→fala, senão fala→silêncio) mais próxima, em janela curta. */
    fun nearestEdge(active: BooleanArray, targetMs: Long, windowMs: Int, onset: Boolean): Long? {
        val center = (targetMs / FRAME).toInt()
        val window = windowMs / FRAME
        var best: Int? = null
        for (f in (center - window).coerceAtLeast(3)..(center + window).coerceAtMost(active.size - 4)) {
            val edge = if (onset) !active[f - 1] && !active[f - 2] && active[f] && active[f + 1] && active[f + 2]
                else active[f - 1] && active[f - 2] && !active[f] && !active[f + 1]
            if (!edge) continue
            if (best == null || abs(f - center) < abs(best - center)) best = f
        }
        return best?.let { it.toLong() * FRAME }
    }

    /** Ajusta cada palavra às pausas reais; tudo-ou-nada por legenda: se algo ficar incoerente, mantém o original. */
    fun refineWords(cue: SubtitleCue, active: BooleanArray): Pair<SubtitleCue, Int> {
        val t = cue.wordTimings
        if (t.isEmpty()) return cue to 0
        val s = LongArray(t.size) { t[it].startMs }
        val e = LongArray(t.size) { t[it].endMs }
        var changed = 0
        for (i in t.indices) {
            nearestEdge(active, s[i], WORD_WINDOW_MS, true)?.let { c ->
                if (c >= cue.startMs && c <= cue.endMs && c != s[i]) { s[i] = c; changed++ }
            }
            nearestEdge(active, e[i], WORD_WINDOW_MS, false)?.let { c ->
                if (c >= cue.startMs && c <= cue.endMs && c != e[i]) { e[i] = c; changed++ }
            }
        }
        if (changed == 0) return cue to 0
        for (i in t.indices) {
            if (e[i] - s[i] < MIN_WORD_MS) return cue to 0
            if (i > 0 && s[i] < s[i - 1] + MIN_WORD_MS) return cue to 0
        }
        return cue.copy(wordTimings = t.indices.map { SubtitleWordTiming(s[it], e[it]) }) to changed
    }

    fun refine(cues: List<SubtitleCue>, envelope: SubtitleVoiceEnvelope, totalMs: Long): Result {
        if (cues.isEmpty() || totalMs <= 0L) return Result(cues, 0L, 0)
        val active = activity(envelope.levels)
        if (active.none { it }) return Result(cues, 0L, 0)
        val globalShift = estimateShift(cues, active)
        val shifted = cues.sortedBy { it.startMs }.map { shift(it, globalShift, totalMs) }
        val out = ArrayList<SubtitleCue>(shifted.size)
        var snapped = 0
        var words = 0
        for ((index, cue) in shifted.withIndex()) {
            val previousEnd = out.lastOrNull()?.endMs ?: 0L
            val nextStart = shifted.getOrNull(index + 1)?.startMs ?: totalMs
            var start = cue.startMs
            var end = cue.endMs
            nearestOnset(active, start)?.let { if (it >= previousEnd && it < end - MIN_CUE_MS) start = it }
            nearestOffset(active, end)?.let { if (it <= nextStart && it <= totalMs && it > start + MIN_CUE_MS) end = it }
            val timed = if ((start != cue.startMs || end != cue.endMs) && end - start >= MIN_CUE_MS) {
                snapped++
                retime(cue, start, end)
            } else cue
            val (precise, changedWords) = refineWords(timed, active)
            words += changedWords
            out += precise
        }
        return Result(out, globalShift, snapped, words)
    }
}
