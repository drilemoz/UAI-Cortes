package br.com.uaicortes

import java.text.Normalizer
import java.util.Locale

/** Keep audio anchors in unchanged words; new words never gain measured timing. */
internal object SubtitleTextRetiming {
    data class Edit(val cue: SubtitleCue, val needsAlignment: Boolean)
    private val words = Regex("\\S+")
    private val punctuation = Regex("^\\p{P}+|\\p{P}+$")
    private fun tokens(text: String) = words.findAll(text).map {
        Normalizer.normalize(it.value, Normalizer.Form.NFC)
            .lowercase(Locale.ROOT).replace(punctuation, "")
    }.toList()

    fun edit(cue: SubtitleCue, text: String): Edit {
        if (text == cue.text) return Edit(cue, false)
        val before = tokens(cue.text)
        val after = tokens(text)
        val times = cue.wordTimings
        if (before == after) return Edit(cue.copy(text = text), false)
        val limit = minOf(before.size, after.size)
        val prefix = (0 until limit).takeWhile { before[it] == after[it] }.count()
        val suffix = (0 until limit - prefix).takeWhile {
            before[before.lastIndex - it] == after[after.lastIndex - it]
        }.count()
        val keptColors = cue.wordColors.mapNotNull { color ->
            when {
                color.index < prefix -> color
                color.index >= before.size - suffix && color.index in before.indices ->
                    color.copy(index = color.index + after.size - before.size)
                else -> null
            }
        }
        if (times.size != before.size || times.isEmpty() || times.any {
                it.startMs < cue.startMs || it.endMs > cue.endMs || it.endMs <= it.startMs
            }) return Edit(cue.copy(text = text, wordTimings = emptyList(), wordColors = keptColors), true)
        val count = after.size - prefix - suffix
        val left = times.take(prefix)
        val right = times.takeLast(suffix)
        val gapFrom = left.lastOrNull()?.endMs ?: cue.startMs
        val gapUntil = right.firstOrNull()?.startMs ?: cue.endMs
        val middle = when {
            count == 0 -> emptyList()
            count == before.size - prefix - suffix -> times.subList(prefix, before.size - suffix)
            gapUntil - gapFrom >= count -> List(count) { i ->
                SubtitleWordTiming(gapFrom + (gapUntil - gapFrom) * i / count,
                    gapFrom + (gapUntil - gapFrom) * (i + 1) / count)
            }
            else -> return Edit(cue.copy(text = text, wordTimings = emptyList(), wordColors = keptColors), true)
        }
        return Edit(cue.copy(text = text, wordTimings = left + middle + right, wordColors = keptColors), true)
    }
}
