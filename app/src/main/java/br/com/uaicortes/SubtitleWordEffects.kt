package br.com.uaicortes

import org.json.JSONArray
import org.json.JSONObject

/** Horários absolutos fornecidos pelo reconhecedor; nenhum horário é inventado. */
data class SubtitleWordTiming(val startMs: Long, val endMs: Long) {
    fun json(): JSONObject = JSONObject().put("startMs", startMs).put("endMs", endMs)
    companion object {
        fun read(json: JSONObject): SubtitleWordTiming = SubtitleWordTiming(
            json.optLong("startMs"), json.optLong("endMs"))
    }
}

/** Cor associada à posição da palavra, não a sua grafia (repetições podem ter cores diferentes). */
data class SubtitleWordColor(val index: Int, val color: Int) {
    fun json(): JSONObject = JSONObject().put("index", index).put("color", color)
    companion object {
        fun read(json: JSONObject) = SubtitleWordColor(json.optInt("index", -1), json.optInt("color", -1))
    }
}

/** A mesma resolução de spans é usada pela prévia Compose e pelo MP4 Media3. */
object SubtitleWordEffects {
    const val DEFAULT_HIGHLIGHT = -256 // amarelo opaco (0xFFFFFF00)
    private val wordPattern = Regex("\\S+")
    data class ColorRange(val range: IntRange, val color: Int)

    fun wordRanges(text: String): List<IntRange> = wordPattern.findAll(text).map { it.range }.toList()

    /** Mistura ARGB de forma determinística; não depende de Android ou do FPS. */
    fun blendColor(from: Int, to: Int, fraction: Float): Int {
        val amount = if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else 0f
        fun channel(shift: Int): Int {
            val a = (from ushr shift) and 0xFF
            val b = (to ushr shift) and 0xFF
            return (a + (b - a) * amount).toInt().coerceIn(0, 255)
        }
        return (channel(24) shl 24) or (channel(16) shl 16) or
            (channel(8) shl 8) or channel(0)
    }

    fun colors(cue: SubtitleCue, visibleText: String, activeWord: IntRange?,
        activeColor: Int = cue.highlightColor): List<ColorRange> {
        val words = wordRanges(visibleText)
        val result = cue.wordColors.asSequence()
            .filter { it.index in words.indices }
            .map { ColorRange(words[it.index], it.color) }.toMutableList()
        if (activeWord != null && activeWord.first >= 0 && activeWord.last < visibleText.length) {
            // A palavra falada tem prioridade sobre a coloração fixa.
            result.removeAll { it.range == activeWord }
            result += ColorRange(activeWord, activeColor)
        }
        return result
    }

    /** Só usa os tempos se houver correspondência 1:1 e intervalos íntegros. */
    fun activeWordIndex(cue: SubtitleCue, atMs: Long): Int? {
        val ranges = wordRanges(cue.text)
        val timings = cue.wordTimings
        if (timings.size != ranges.size || timings.isEmpty() ||
            timings.any { it.startMs < cue.startMs || it.endMs > cue.endMs || it.endMs <= it.startMs } ||
            timings.zipWithNext().any { (a,b) -> a.startMs > b.startMs }) return null
        val active = timings.indexOfLast { atMs >= it.startMs && atMs < it.endMs }
        if (active >= 0) return active
        // Nas pausas, mantém o destaque da última palavra já pronunciada.
        return timings.indexOfLast { atMs >= it.endMs }.takeIf { it >= 0 }
            ?: 0
    }

    fun safeTimings(json: JSONArray?, startMs: Long, endMs: Long, count: Int): List<SubtitleWordTiming> {
        if (json == null || count <= 0 || count > 100) return emptyList()
        val result = (0 until json.length().coerceAtMost(100)).mapNotNull {
            json.optJSONObject(it)?.let(SubtitleWordTiming::read)
        }
        if (result.size != count || result.any { it.startMs < startMs || it.endMs > endMs || it.endMs <= it.startMs } ||
            result.zipWithNext().any { (a,b) -> a.startMs > b.startMs }) return emptyList()
        return result
    }

    fun safeColors(json: JSONArray?, count: Int): List<SubtitleWordColor> {
        if (json == null || count <= 0) return emptyList()
        return (0 until json.length().coerceAtMost(100)).mapNotNull {
            json.optJSONObject(it)?.let(SubtitleWordColor::read)
        }.filter { it.index in 0 until count }.distinctBy { it.index }
    }
}
