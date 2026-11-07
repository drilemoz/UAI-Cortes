package br.com.uaicortes

import br.com.uaicortes.subtitles.core.SubtitleTextCasePolicy
import br.com.uaicortes.subtitles.core.SubtitleMotionPolicy
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
    const val KARAOKE_PULSE = "karaoke_pulse"
    const val KARAOKE_BOLD = "karaoke_bold"
    const val KARAOKE_UNDERLINE = "karaoke_underline"
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
    const val SOFT_POP = SubtitleMotionPolicy.SOFT_POP
    const val CINEMATIC = SubtitleMotionPolicy.CINEMATIC
    const val SOFT_FADE = SubtitleMotionPolicy.SOFT_FADE
    const val IMPACT = SubtitleMotionPolicy.IMPACT
    const val GLIDE = SubtitleMotionPolicy.GLIDE
    val choices = listOf(
        NONE to "Sem efeito", TYPEWRITER to "Letra por letra",
        PHRASES to "Frase por frase", KARAOKE to "Palavra destacada",
        KARAOKE_PULSE to "Pulso de cor", KARAOKE_BOLD to "Palavra forte",
        KARAOKE_UNDERLINE to "Sublinhar palavra",
        POP to "Pop / zoom", FADE to "Aparecer / sumir", SLIDE to "Deslizar",
        BOUNCE to "Bounce", PULSE to "Pulsar", ZOOM_OUT to "Zoom reverso",
        RISE to "Subir", DROP to "Descer",
        SLIDE_LEFT to "Entrar esquerda", SLIDE_RIGHT to "Entrar direita",
        WORD_REVEAL to "Palavra por palavra", ELASTIC to "Elástico",
        FLOAT to "Flutuar", SWAY to "Balançar", SOFT_SHAKE to "Tremor suave",
        SOFT_POP to "Pop suave", CINEMATIC to "Cinema suave",
        SOFT_FADE to "Fade delicado", IMPACT to "Impacto limpo",
        GLIDE to "Deslizar suave"
    )
    /** A organização por categorias evita uma única lista horizontal com dezenas de efeitos. */
    val groups: List<Pair<String, List<String>>> = listOf(
        "Entrada" to listOf(NONE, POP, FADE, SLIDE, ZOOM_OUT, RISE, DROP, SLIDE_LEFT, SLIDE_RIGHT, ELASTIC, SOFT_POP, CINEMATIC, SOFT_FADE, GLIDE),
        "Texto" to listOf(TYPEWRITER, PHRASES, WORD_REVEAL, KARAOKE,
            KARAOKE_PULSE, KARAOKE_BOLD, KARAOKE_UNDERLINE),
        "Movimento" to listOf(BOUNCE, PULSE, FLOAT, SWAY, SOFT_SHAKE, IMPACT)
    )
    fun label(id: String): String = choices.firstOrNull { it.first == id }?.second ?: "Sem efeito"
    fun isKaraoke(id: String): Boolean = normalize(id) in setOf(
        KARAOKE, KARAOKE_PULSE, KARAOKE_BOLD, KARAOKE_UNDERLINE)
    fun normalize(value: String): String = if (choices.any { it.first == value }) value else NONE
    fun speed(value: Float): Float = if (value.isFinite()) value.coerceIn(.5f, 2f) else 1f

    data class Frame(val text: String, val highlighted: IntRange? = null,
        val alpha: Float = 1f, val scale: Float = 1f, val slideY: Float = 0f,
        val slideX: Float = 0f, val highlightColorOverride: Int? = null,
        val boldActiveWord: Boolean = false, val underlineActiveWord: Boolean = false)

    fun frame(cue: SubtitleCue, absoluteMs: Long): Frame {
        val text = SubtitleTextCasePolicy.display(cue.text, cue.textCase)
        val elapsed = (absoluteMs - cue.startMs).coerceAtLeast(0L)
        val duration = (cue.endMs - cue.startMs).coerceAtLeast(1L)
        val ratio = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
        val rate = speed(cue.animationSpeed)
        // Entrada em 15 a 25% da duração, limitada a 450 ms, ajustável.
        val entranceMs = (minOf(duration * .25f, 450f) / rate).coerceAtLeast(1f)
        val enter = (elapsed / entranceMs).coerceIn(0f, 1f)
        return when (normalize(cue.animation)) {
            SOFT_POP, CINEMATIC, SOFT_FADE, IMPACT, GLIDE -> {
                val motion = SubtitleMotionPolicy.evaluate(cue.animation, elapsed, duration, rate)
                Frame(text, alpha = motion.alpha, scale = motion.scale,
                    slideX = motion.slideX, slideY = motion.slideY)
            }
            TYPEWRITER -> {
                val count = (text.length * (ratio * rate).coerceIn(0f, 1f)).roundToInt()
                Frame(text.take(count))
            }
            PHRASES -> {
                val parts = splitPhrases(text)
                val idx = ((ratio * rate).coerceIn(0f, .999999f) * parts.size).toInt()
                Frame(parts.getOrElse(idx) { "" })
            }
            KARAOKE, KARAOKE_PULSE, KARAOKE_BOLD, KARAOKE_UNDERLINE -> {
                val words = Regex("\\S+").findAll(text).toList()
                if (words.isEmpty()) Frame(text) else {
                    val exact = SubtitleWordEffects.activeWordIndex(cue, absoluteMs)
                    val idx = (exact ?: ((ratio * rate).coerceIn(0f, .999999f) * words.size).toInt())
                        .coerceIn(0, words.lastIndex)
                    val mode = normalize(cue.animation)
                    // O pulso altera a COR, não a largura do texto; evita quebras ou saltos na linha.
                    // Quantização reduz reconstruções de bitmap no Media3 durante o efeito.
                    val pulseColor = if (mode == KARAOKE_PULSE) {
                        val timing = cue.wordTimings.getOrNull(idx)?.takeIf {
                            cue.wordTimings.size == words.size &&
                                SubtitleWordEffects.activeWordIndex(cue, absoluteMs) != null
                        }
                        val progress = if (timing != null) {
                            ((absoluteMs - timing.startMs).toFloat() /
                                (timing.endMs - timing.startMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
                        } else ((ratio * rate * words.size) - idx).coerceIn(0f, 1f)
                        val beat = (0.5f - 0.5f * kotlin.math.cos(progress * 2f * PI).toFloat())
                        val step = (beat * 6f).roundToInt() / 6f
                        SubtitleWordEffects.blendColor(cue.color, cue.highlightColor, 0.4f + 0.6f * step)
                    } else null
                    Frame(text, words[idx].range, highlightColorOverride=pulseColor,
                        boldActiveWord=mode == KARAOKE_BOLD,
                        underlineActiveWord=mode == KARAOKE_UNDERLINE)
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
