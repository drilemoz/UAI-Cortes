package br.com.uaicortes

import kotlin.math.roundToLong

/** Passos temporais baseados no FPS nominal do clipe.
 * Media3.seekTo() não garante navegação exata por frames, especialmente em VFR.
 */
internal object FrameStepPolicy {
    const val FALLBACK_FPS = 30f
    fun fpsOrFallback(fps: Float?): Float =
        if (fps != null && fps.isFinite() && fps in 1f..240f) fps else FALLBACK_FPS

    fun intervalMs(fps: Float?): Long =
        (1000.0 / fpsOrFallback(fps).toDouble()).roundToLong().coerceAtLeast(1L)

    fun target(positionMs: Long, totalMs: Long, direction: Int, fps: Float?): Long {
        require(direction == -1 || direction == 1)
        val delta = intervalMs(fps) * direction
        return TimelinePrecision.step(positionMs, delta, totalMs)
    }
}
