package br.com.uaicortes

import kotlin.math.min

/** Configuração pura, sem rede ou IA. Imagens sempre mantêm a proporção original. */
data class ImageAnimationOptions(
    val intensity: String = "normal",
    val secondsPerImage: Int = 5,
    val aspect: String = "9:16",
    val quality: Int = 720,
    val fps: Int = 30,
    val transition: String = "auto"
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
    }
}

data class CameraFrame(val scale: Float, val panX: Float, val panY: Float)

object ImageAnimationPlan {
    const val MAX_IMAGES = 80
    fun ease(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f*x)
    }
    fun frame(index: Int, fraction: Float, intensity: String): CameraFrame {
        val t = ease(fraction)
        val zoom = when (intensity) { "suave" -> .045f; "intensa" -> .14f; else -> .085f }
        // Pan sempre menor que a margem produzida pelo zoom; não expõe bordas.
        val shift = zoom * .37f
        val mode = Math.floorMod(index, 6)
        val (z0, z1) = if (mode % 2 == 0) 1f + zoom*.25f to 1f + zoom
            else 1f + zoom to 1f + zoom*.25f
        val xDirection = when (mode) { 0,4 -> 1f; 2 -> -1f; else -> 0f }
        val yDirection = when (mode) { 1,5 -> 1f; 3 -> -1f; else -> 0f }
        val scale = z0 + (z1-z0)*t
        // A posição extrema acompanha a margem disponível EM CADA frame.
        val margin = (scale - 1f).coerceAtLeast(0f) / scale
        val drift = ((2*t-1f) * shift).coerceIn(-margin*.75f,margin*.75f)
        return CameraFrame(scale, xDirection*drift, yDirection*drift)
    }
    fun transitionKind(options: ImageAnimationOptions, index: Int): String = when (options.transition) {
        "corte" -> "Corte direto"
        "preto" -> "Fade preto"
        "branco" -> "Fade branco"
        else -> if (index % 4 == 3) "Fade branco" else "Fade preto"
    }
    fun fadeMs(options: ImageAnimationOptions): Long = min(350L, options.durationMs / 8)
    fun totalDuration(images: Int, options: ImageAnimationOptions): Long =
        images.coerceIn(0,MAX_IMAGES) * options.durationMs
}
