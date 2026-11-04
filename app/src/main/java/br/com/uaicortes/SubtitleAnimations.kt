package br.com.uaicortes

import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.PI

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
    const val BOUNCE = "bounce"
    const val PULSE = "pulse"
    const val ZOOM_OUT = "zoom_out"
    const val RISE = "rise"
    const val DROP = "drop"
    const val SLIDE_LEFT = "slide_left"
    const val SLIDE_RIGHT = "slide_right"
    const val WORD_REVEAL = "word_reveal"
    const val ELASTIC = "elastic"
    const val FLOAT = "float"
    const val SWAY = "sway"
    const val SOFT_SHAKE = "soft_shake"
    val choices = listOf(
        NONE to "Sem efeito", TYPEWRITER to "Letra por letra",
        PHRASES to "Frase por frase", KARAOKE to "Palavra destacada",
        POP to "Pop / zoom", FADE to "Aparecer / sumir", SLIDE to "Deslizar",
        BOUNCE to "Bounce", PULSE to "Pulsar", ZOOM_OUT to "Zoom reverso",
        RISE to "Subir", DROP to "Descer",
        SLIDE_LEFT to "Entrar esquerda", SLIDE_RIGHT to "Entrar direita",
        WORD_REVEAL to "Palavra por palavra", ELASTIC to "Elástico",
        FLOAT to "Flutuar", SWAY to "Balançar", SOFT_SHAKE to "Tremor suave"
    )
    /** A organização por categorias evita uma única lista horizontal com dezenas de efeitos. */
    val groups: List<Pair<String, List<String>>> = listOf(
        "Entrada" to listOf(NONE, POP, FADE, SLIDE, ZOOM_OUT, RISE, DROP, SLIDE_LEFT, SLIDE_RIGHT, ELASTIC),
        "Texto" to listOf(TYPEWRITER, PHRASES, WORD_REVEAL, KARAOKE),
        "Movimento" to listOf(BOUNCE, PULSE, FLOAT, SWAY, SOFT_SHAKE)
    )
    fun label(id: String): String = choices.firstOrNull { it.first == id }?.second ?: "Sem efeito"
    fun normalize(value: String): String = if (choices.any { it.first == value }) value else NONE
    fun speed(value: Float): Float = if (value.isFinite()) value.coerceIn(.5f, 2f) else 1f

    data class Frame(val text: String, val highlighted: IntRange? = null,
        val alpha: Float = 1f, val scale: Float = 1f, val slideY: Float = 0f,
        val slideX: Float = 0f)

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
                    val exact = SubtitleWordEffects.activeWordIndex(cue, absoluteMs)
                    val idx = exact ?: ((ratio * rate).coerceIn(0f, .999999f) * words.size).toInt()
                    Frame(text, words[idx.coerceIn(0,words.lastIndex)].range)
                }
            }
            WORD_REVEAL -> {
                val words = Regex("\\S+").findAll(text).toList()
                if (words.isEmpty()) Frame(text) else {
                    val completed = (words.size * (ratio * rate).coerceIn(0f, 1f)).toInt()
                    Frame(if (completed == 0) "" else text.take(words[completed - 1].range.last + 1))
                }
            }
            RISE -> Frame(text, slideY = .16f * (1f - enter), alpha = enter)
            DROP -> Frame(text, slideY = -.16f * (1f - enter), alpha = enter)
            SLIDE_LEFT -> Frame(text, slideX = -.17f * (1f - enter), alpha = enter)
            SLIDE_RIGHT -> Frame(text, slideX = .17f * (1f - enter), alpha = enter)
            ELASTIC -> Frame(text, scale = .7f + .3f * enter +
                .20f * sin(enter * PI * 2.0).toFloat() * (1f - enter))
            FLOAT -> Frame(text, slideY = .020f *
                sin(elapsed.toDouble() * 2.0 * PI / (1150.0 / rate)).toFloat())
            SWAY -> Frame(text, slideX = .018f *
                sin(elapsed.toDouble() * 2.0 * PI / (970.0 / rate)).toFloat())
            SOFT_SHAKE -> Frame(text, slideX = .028f *
                sin(enter * PI * 7.0).toFloat() * (1f - enter))
            POP -> Frame(text, scale = .75f + .25f * enter)
            FADE -> {
                val exit = ((duration - elapsed) / entranceMs).coerceIn(0f, 1f)
                Frame(text, alpha = minOf(enter, exit))
            }
            SLIDE -> Frame(text, slideY = .20f * (1f - enter))
            BOUNCE -> Frame(text, scale = .65f + .35f*enter +
                .18f * sin(enter * PI).toFloat())
            PULSE -> Frame(text, scale = 1f + .06f *
                sin(elapsed.toDouble() * 2.0 * PI / (650.0 / rate)).toFloat())
            ZOOM_OUT -> Frame(text, scale = 1.3f - .3f * enter)
            else -> Frame(text)
        }
    }

    /** Separa em frases por pontuação ou quebra de linha, sem alterar original. */
    fun splitPhrases(text: String): List<String> =
        text.split(Regex("(?<=[.!?;])\\s+|\\n+")).map(String::trim)
            .filter(String::isNotBlank).ifEmpty { listOf(text) }
}
