package br.com.uaicortes

import kotlin.math.min

/** Configuração pura, sem rede ou IA. Imagens sempre mantêm a proporção original. */
data class ImageAnimationOptions(
    val intensity: String = "normal",
    val secondsPerImage: Int = 5,
    val aspect: String = "9:16",
    val quality: Int = 720,
    val fps: Int = 30,
    val transition: String = "auto",
    val motion: String = "auto"
) {
    val width: Int get() = when (aspect) { "16:9" -> if (quality == 1080) 1920 else 1280
        "1:1" -> quality; else -> quality }
    val height: Int get() = when (aspect) { "16:9" -> quality; "1:1" -> quality
        else -> if (quality == 1080) 1920 else 1280 }
    val durationMs: Long get() = secondsPerImage * 1000L
    fun checked(): ImageAnimationOptions = apply {
        require(intensity in listOf("suave", "normal", "intensa"))
        require(secondsPerImage in listOf(3, 5, 8))
        require(aspect in listOf("9:16", "16:9", "1:1"))
        require(quality in listOf(720, 1080))
        require(fps == 30)
        require(transition in listOf("auto", "corte", "preto", "branco"))
        require(motion in listOf("auto", "zoom_in", "zoom_out", "horizontal", "vertical"))
    }
}

data class CameraFrame(val scale: Float, val panX: Float, val panY: Float)

object ImageAnimationPlan {
    const val MAX_IMAGES = 80
    fun ease(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f*x)
    }
    /** Presets determinísticos. A mesma função governa a prévia Compose e o Media3. */
    fun frame(index: Int, fraction: Float, intensity: String, motion: String = "auto"): CameraFrame {
        val t = ease(fraction)
        val zoom = when (intensity) { "suave" -> .045f; "intensa" -> .14f; else -> .085f }
        val mode = Math.floorMod(index, 6)
        val start = 1f + zoom * .25f
        val end = 1f + zoom
        val (scale, xDirection, yDirection, driftFactor) = when (motion) {
            "zoom_in" -> FramePreset(start + (end - start) * t, 0f, 0f, 0f)
            "zoom_out" -> FramePreset(end + (start - end) * t, 0f, 0f, 0f)
            "horizontal" -> FramePreset(end, if (index % 2 == 0) 1f else -1f, 0f, 2*t - 1f)
            "vertical" -> FramePreset(end, 0f, if (index % 2 == 0) 1f else -1f, 2*t - 1f)
            else -> FramePreset(
                if (mode % 2 == 0) start + (end - start) * t else end + (start - end) * t,
                when (mode) { 0,4 -> 1f; 2 -> -1f; else -> 0f },
                when (mode) { 1,5 -> 1f; 3 -> -1f; else -> 0f }, 2*t - 1f
            )
        }
        // Limite conservador do enquadramento, inclusive nas extremidades do movimento.
        val margin = (scale - 1f).coerceAtLeast(0f) / scale
        val drift = (driftFactor * zoom * .37f).coerceIn(-margin*.75f, margin*.75f)
        return CameraFrame(scale, xDirection * drift, yDirection * drift)
    }
    private data class FramePreset(val scale: Float, val x: Float, val y: Float, val drift: Float)

    fun transitionKind(options: ImageAnimationOptions, index: Int): String = when (options.transition) {
        "corte" -> "Corte direto"
        "preto" -> "Fade preto"
        "branco" -> "Fade branco"
        else -> if (index % 4 == 3) "Fade branco" else "Fade preto"
    }
    fun fadeMs(options: ImageAnimationOptions): Long = min(350L, options.durationMs / 8)
    /** Ao selecionar uma foto, mostra o meio da cena em vez do primeiro quadro do fade. */
    fun focusTimeMs(index: Int, options: ImageAnimationOptions): Long =
        index.coerceIn(0, MAX_IMAGES - 1) * options.durationMs + options.durationMs / 2
    fun totalDuration(images: Int, options: ImageAnimationOptions): Long =
        images.coerceIn(0,MAX_IMAGES) * options.durationMs
}
