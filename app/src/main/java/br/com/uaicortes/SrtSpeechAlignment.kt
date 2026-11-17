package br.com.uaicortes

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/** O texto e os IDs do SRT são intocáveis. Só horários de frases com correspondência firme mudam. */
internal object SrtSpeechAlignment {
    data class Result(val cues: List<SubtitleCue>, val matched: Int, val adjusted: Int)
    private data class Word(val token: String, val timing: SubtitleWordTiming)
    private val words = Regex("\\S+")
    private val tags = Regex("<[^>]+>")
    private val marks = Regex("\\p{M}+")
    private val punctuation = Regex("[^\\p{L}\\p{N}]")
    private fun token(text: String): String = Normalizer.normalize(tags.replace(text, ""), Normalizer.Form.NFD)
        .replace(marks, "").lowercase(Locale.ROOT).replace(punctuation, "")

    fun align(imported: List<SubtitleCue>, spoken: List<SubtitleCue>,
        projectDurationMs: Long, offsetMs: Long = 0L): Result {
        val transcript = spoken.flatMap { cue ->
            val tokens = words.findAll(cue.text).map { token(it.value) }.toList()
            if (tokens.size != cue.wordTimings.size) emptyList()
            else tokens.zip(cue.wordTimings).mapNotNull { (value, time) ->
                val start = time.startMs + offsetMs
                val end = time.endMs + offsetMs
                if (value.isEmpty() || start < 0L || end <= start || end > projectDurationMs) null
                else Word(value, SubtitleWordTiming(start, end))
            }
        }.distinct().sortedBy { it.timing.startMs }
        if (transcript.isEmpty()) return Result(imported, 0, 0)
        val byToken = transcript.indices.groupBy { transcript[it].token }
        val ordered = imported.withIndex().sortedBy { it.value.startMs }
        val proposals = mutableMapOf<Int, SubtitleCue>()
        for (indexed in ordered) {
            val cue = indexed.value
            val tokens = words.findAll(cue.text).map { token(it.value) }.toList()
            if (tokens.isEmpty() || tokens.size > 100 || tokens.any(String::isBlank)) continue
            val limit = if (tokens.size == 1) 650L else 4_000L
            val expected = cue.startMs
            val candidates = byToken[tokens.first()].orEmpty().filter { i ->
                i + tokens.size <= transcript.size && abs(transcript[i].timing.startMs - expected) <= limit &&
                    tokens.indices.all { j ->
                        val present = transcript[i + j]
                        present.token == tokens[j] && (j == 0 ||
                            present.timing.startMs - transcript[i + j - 1].timing.endMs in -100L..800L)
                    } && transcript[i + tokens.lastIndex].timing.endMs - transcript[i].timing.startMs <=
                        (cue.endMs - cue.startMs).coerceAtLeast(300L) + 2_000L
            }.sortedBy { abs(transcript[it].timing.startMs - expected) }
            val best = candidates.firstOrNull() ?: continue
            if (candidates.size > 1 && (tokens.size == 1 ||
                    abs(transcript[candidates[1]].timing.startMs - expected) -
                    abs(transcript[best].timing.startMs - expected) < 450L)) continue
            val timings = tokens.indices.map { transcript[best + it].timing }
            val start = timings.first().startMs
            val end = timings.maxOf { it.endMs }
            if (end > start) proposals[indexed.index] =
                cue.copy(startMs = start, endMs = end, wordTimings = timings)
        }
        // Compare propostas juntas para aceitar um desvio consistente, sem criar sobreposições.
        // Ao reverter um par, confira também seus vizinhos que agora voltam a ver o SRT original.
        var hasConflict: Boolean
        do {
            val conflict = mutableSetOf<Int>()
            for ((left, right) in ordered.zipWithNext()) {
                if (left.value.endMs > right.value.startMs) continue // sobreposição já existente
                val adjustedLeft = proposals[left.index] ?: left.value
                val adjustedRight = proposals[right.index] ?: right.value
                if (adjustedLeft.endMs > adjustedRight.startMs) {
                    if (left.index in proposals) conflict += left.index
                    if (right.index in proposals) conflict += right.index
                }
            }
            conflict.forEach { proposals.remove(it) }
            hasConflict = conflict.isNotEmpty()
        } while (hasConflict)
        val adjusted = proposals.count { (index, cue) ->
            cue.startMs != imported[index].startMs || cue.endMs != imported[index].endMs
        }
        return Result(imported.mapIndexed { index, cue -> proposals[index] ?: cue }, proposals.size, adjusted)
    }
}
