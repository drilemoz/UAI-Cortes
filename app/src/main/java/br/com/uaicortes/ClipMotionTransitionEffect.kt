package br.com.uaicortes

import android.graphics.Matrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation

/** Movimento real por frame no Transformer, sem fingir crossfade entre dois vídeos.
 * Escala >= 1 mantém as bordas preenchidas durante o movimento.
 */
@UnstableApi
class ClipMotionTransitionEffect(
    private val durationMs: Long,
    private val incomingKind: String, private val incomingMs: Long,
    private val outgoingKind: String, private val outgoingMs: Long
) : MatrixTransformation {
    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val t = (presentationTimeUs / 1000L).coerceIn(0L, durationMs.coerceAtLeast(0L))
        val entering = if (incomingMs > 0) TransitionEffects.intensity(incomingKind,
            (incomingMs - t).toFloat() / incomingMs) else 0f
        val leaving = if (outgoingMs > 0) TransitionEffects.intensity(outgoingKind,
            (t - (durationMs - outgoingMs)).toFloat() / outgoingMs) else 0f
        val kind = if (entering >= leaving) incomingKind else outgoingKind
        val strength = maxOf(entering, leaving)
        val scale = TransitionEffects.motionScale(kind, strength)
        val pan = TransitionEffects.motionX(kind, strength)
        return Matrix().apply {
            setScale(scale, scale)
            postTranslate(pan, 0f)
        }
    }
}
