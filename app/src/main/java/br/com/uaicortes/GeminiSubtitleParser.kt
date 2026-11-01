package br.com.uaicortes

import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

/** Parsing verificável das anotações `word_info` da Interactions API.
 * Não inventa horários quando a resposta não contém marcações por palavra.
 */
internal object GeminiSubtitleParser {
    private data class Word(val text: String,val startMs: Long,val endMs: Long)
    private fun ms(value: String): Long? {
        if(!value.matches(Regex("[0-9]+(?:\\.[0-9]+)?s"))) return null
        return runCatching { BigDecimal(value.removeSuffix("s"))
            .multiply(BigDecimal(1000)).setScale(0,RoundingMode.HALF_UP).longValueExact() }.getOrNull()
    }

    fun parse(response: JSONObject, totalMs: Long): List<SubtitleCue> {
        require(totalMs > 0)
        val words= mutableListOf<Word>()
        val steps=response.optJSONArray("steps")
        if(steps!=null) for(i in 0 until steps.length()) {
            val content=steps.optJSONObject(i)?.optJSONArray("content") ?: continue
            for(j in 0 until content.length()) {
                val annotations=content.optJSONObject(j)?.optJSONArray("annotations") ?: continue
                for(k in 0 until annotations.length()) {
                    val item=annotations.optJSONObject(k) ?: continue
                    if(item.optString("type") != "word_info") continue
                    val word=item.optString("text").trim()
                    val start=ms(item.optString("start_offset"))
                    val end=ms(item.optString("end_offset"))
                    if(word.isNotBlank() && SubtitleWordEffects.wordRanges(word).size==1 &&
                        start!=null && end!=null && end>start && start < totalMs && start>=0) {
                        val clipped=end.coerceAtMost(totalMs)
                        if(clipped > start) words+=Word(word.take(70),start,clipped)
                    }
                }
            }
        }
        require(words.isNotEmpty()) {
            "Gemini não devolveu horários por palavra. Nenhuma legenda foi aplicada."
        }
        require(words.size <= 10000) { "Gemini retornou palavras demais." }
        val sorted=words.sortedBy { it.startMs }
        val result=mutableListOf<SubtitleCue>()
        var buffer= mutableListOf<Word>()
        fun flush() {
            if(buffer.isEmpty()) return
            val visible=buffer.joinToString(" ") { it.text }
            val start=buffer.first().startMs
            val end=buffer.last().endMs
            if(end>start && visible.length<=170 && result.size<600) {
                result+=SubtitleCue(text=visible,startMs=start,endMs=end,sizeSp=30,
                    backgroundColor=SubtitleStyles.NO_BACKGROUND,outlineWidth=3,
                    shadowOffsetDp=2,animation=SubtitleAnimations.KARAOKE,
                    wordTimings=buffer.map { SubtitleWordTiming(it.startMs,it.endMs) })
            }
            buffer=mutableListOf()
        }
        for(word in sorted) {
            if(buffer.isNotEmpty() && (buffer.size>=7 ||
                word.startMs-buffer.last().endMs>750 ||
                word.endMs-buffer.first().startMs>3600 ||
                buffer.sumOf { it.text.length+1 } + word.text.length >170)) flush()
            if(result.size>=600) break
            buffer+=word
        }
        flush()
        return result
    }
}
