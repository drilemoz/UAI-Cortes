package br.com.uaicortes

import kotlin.math.max
import kotlin.math.min

/** Heurística local de energia do sinal, NÃO é reconhecimento de fala nem alinhamento por palavra. */
internal object ReferenceAudioActivity {
    const val BINS = 144
    const val WINDOW_BEFORE_MS = 3_000L
    const val WINDOW_AFTER_MS = 2_000L
    const val MIN_REGION_MS = 180L

    data class Window(val fromMs: Long, val untilMs: Long)
    data class Region(val startMs: Long, val endMs: Long)
    data class Result(
        val fromMs: Long,
        val untilMs: Long,
        val levels: List<Float>,
        val regions: List<Region>,
        val suggestedStartMs: Long?
    )

    fun window(cueStartMs: Long, cueEndMs: Long, projectDurationMs: Long): Window {
        val end = projectDurationMs.coerceAtLeast(1L)
        val from = (cueStartMs - WINDOW_BEFORE_MS).coerceIn(0L, end - 1L)
        // Limitar uma análise a 15 segundos para proteger CPU/bateria e evitar decodificação grande.
        val until = (cueEndMs + WINDOW_AFTER_MS).coerceAtMost(end)
            .coerceAtMost(from + 15_000L).coerceAtLeast(from + 1L)
        return Window(from, until)
    }

    /** O limiar mistura piso absoluto, ruído de fundo e amplitude de pico. */
    fun detect(levels: List<Float>, fromMs: Long, untilMs: Long, cueStartMs: Long): Result {
        if (levels.isEmpty() || untilMs <= fromMs) return Result(fromMs, untilMs, levels, emptyList(), null)
        val clean = levels.map { it.coerceIn(0f, 1f) }
        val sorted = clean.sorted()
        val background = sorted[(sorted.lastIndex * 0.20).toInt()]
        val high = sorted[(sorted.lastIndex * 0.95).toInt()]
        val threshold = max(0.022f, max(background * 2.8f, high * 0.20f))
        val raw = BooleanArray(clean.size) { clean[it] >= threshold }
        // Preencher pausas internas menores que ~120 ms; não confundir um clique isolado com voz.
        val binMs = (untilMs - fromMs).toDouble() / clean.size
        val gapBins = max(1, (120.0 / binMs).toInt())
        var i = 0
        while (i < raw.size) {
            if (raw[i]) { i++; continue }
            val gapStart = i
            while (i < raw.size && !raw[i]) i++
            if (gapStart > 0 && i < raw.size && i - gapStart <= gapBins)
                for (j in gapStart until i) raw[j] = true
        }
        val regions = mutableListOf<Region>()
        i = 0
        while (i < raw.size) {
            if (!raw[i]) { i++; continue }
            val first = i
            while (i < raw.size && raw[i]) i++
            val start = fromMs + (first * binMs).toLong()
            val end = min(untilMs, fromMs + (i * binMs).toLong())
            if (end - start >= MIN_REGION_MS) regions += Region(start, end)
        }
        // Priorizar o começo de uma região próxima à legenda; evitar correções grandes e incertas.
        val candidate = regions.map { it.startMs }
            .filter { it in (cueStartMs - 2_000L)..(cueStartMs + 1_000L) }
            .minByOrNull { kotlin.math.abs(cueStartMs - it) }
            ?.takeIf { kotlin.math.abs(cueStartMs - it) >= 80L }
        return Result(fromMs, untilMs, clean, regions, candidate)
    }
}
