package br.com.uaicortes.subtitles.core

/** Curvas puras para o Subtitle Studio: amostradas pelo tempo absoluto na prévia e no MP4.
 * Sem Android, bibliotecas externas, estados mutáveis ou dependência da taxa de quadros.
 */
object SubtitleMotionPolicy {
    const val SOFT_POP = "soft_pop"
    const val CINEMATIC = "cinematic"
    const val SOFT_FADE = "soft_fade"
    const val IMPACT = "impact"
    const val GLIDE = "glide"

    val styles: Set<String> = setOf(SOFT_POP, CINEMATIC, SOFT_FADE, IMPACT, GLIDE)

    /** Deslocamentos em frações da largura/altura do quadro, usados por ambos os renderizadores. */
    data class Motion(val alpha: Float = 1f, val scale: Float = 1f,
        val slideX: Float = 0f, val slideY: Float = 0f)

    fun evaluate(id: String, elapsedMs: Long, durationMs: Long, speed: Float = 1f): Motion {
        if (id !in styles) return Motion()
        val duration = durationMs.coerceAtLeast(1L)
        val elapsed = elapsedMs.coerceIn(0L, duration).toFloat()
        val rate = if (speed.isFinite()) speed.coerceIn(.5f, 2f) else 1f
        val enterMs = (minOf(duration.toFloat() * .26f, 420f) / rate).coerceAtLeast(1f)
        val exitMs = (minOf(duration.toFloat() * .18f, 240f) / rate).coerceAtLeast(1f)
        val entrance = smoothstep((elapsed / enterMs).coerceIn(0f, 1f))
        val exit = smoothstep(((duration.toFloat() - elapsed) / exitMs).coerceIn(0f, 1f))
        val fade = minOf(entrance, exit)
        val progress = (elapsed / enterMs).coerceIn(0f, 1f)
        val ease = easeOutCubic(progress)
        return when (id) {
            SOFT_POP -> Motion(alpha = fade, scale = .82f + .18f * easeOutBack(progress))
            CINEMATIC -> Motion(alpha = fade, scale = .98f + .02f * ease,
                slideY = .035f * (1f - ease))
            SOFT_FADE -> Motion(alpha = fade)
            IMPACT -> Motion(alpha = fade, scale = 1.14f - .14f * ease,
                slideY = -.024f * (1f - ease))
            GLIDE -> Motion(alpha = fade, slideX = -.09f * (1f - ease))
            else -> Motion()
        }
    }

    private fun smoothstep(x: Float): Float = x * x * (3f - 2f * x)
    private fun easeOutCubic(x: Float): Float {
        val d = 1f - x
        return 1f - d * d * d
    }
    private fun easeOutBack(x: Float): Float {
        val d = x - 1f
        return 1f + 2.2f * d * d * d + 1.2f * d * d
    }
}
