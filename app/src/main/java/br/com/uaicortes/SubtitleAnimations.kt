package br.com.uaicortes

import kotlin.math.roundToInt

/** Efeitos temporais determinísticos usados na prévia e na exportação offline.
 * A duração da legenda delimita cada efeito; não presume alinhamento com áudio.
 */
object SubtitleAnimations {
    const val NONE = "none"
    const val TYPEWRITER = "typewriter"
    const val PHRASES = "phrases"
    const val KARAOKE = "karaoke"
    const val POP = "pop"
    const val FADE = "fade"
    const val SLIDE = "slide"
    val choices = listOf(
        NONE to "Sem efeito", TYPEWRITER to "Letra por letra",
        PHRASES to "Frase por frase", KARAOKE to "Palavra destacada",
        POP to "Pop / zoom", FADE to "Aparecer / sumir", SLIDE to "Deslizar"
    )
    fun normalize(value: String): String = if (choices.any { it.first == value }) value else NONE
    fun speed(value: Float): Float = if (value.isFinite()) value.coerceIn(.5f, 2f) else 1f

    data class Frame(val text: String, val highlighted: IntRange? = null,
        val alpha: Float = 1f, val scale: Float = 1f, val slideY: Float = 0f)

    fun frame(cue: SubtitleCue, absoluteMs: Long): Frame {
        val text = cue.text
        val elapsed = (absoluteMs - cue.startMs).coerceAtLeast(0L)
        val duration = (cue.endMs - cue.startMs).coerceAtLeast(1L)
        val ratio = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
        val rate = speed(cue.animationSpeed)
        // Entrada em 15 a 25% da duração, limitada a 450 ms, ajustável.
        val entranceMs = (minOf(duration * .25f, 450f) / rate).coerceAtLeast(1f)
        val enter = (elapsed / entranceMs).coerceIn(0f, 1f)
        return when (normalize(cue.animation)) {
            TYPEWRITER -> {
                val count = (text.length * (ratio * rate).coerceIn(0f, 1f)).roundToInt()
                Frame(text.take(count))
            }
            PHRASES -> {
                val parts = splitPhrases(text)
                val idx = ((ratio * rate).coerceIn(0f, .999999f) * parts.size).toInt()
                Frame(parts.getOrElse(idx) { "" })
            }
            KARAOKE -> {
                val words = Regex("\\S+").findAll(text).toList()
                if (words.isEmpty()) Frame(text) else {
                    val idx = ((ratio * rate).coerceIn(0f, .999999f) * words.size).toInt()
                    Frame(text, words[idx].range)
                }
            }
            POP -> Frame(text, scale = .75f + .25f * enter)
            FADE -> {
                val exit = ((duration - elapsed) / entranceMs).coerceIn(0f, 1f)
                Frame(text, alpha = minOf(enter, exit))
            }
            SLIDE -> Frame(text, slideY = .20f * (1f - enter))
            else -> Frame(text)
        }
    }

    /** Separa em frases por pontuação ou quebra de linha, sem alterar original. */
    fun splitPhrases(text: String): List<String> =
        text.split(Regex("(?<=[.!?;])\\s+|\\n+")).map(String::trim)
            .filter(String::isNotBlank).ifEmpty { listOf(text) }
}
