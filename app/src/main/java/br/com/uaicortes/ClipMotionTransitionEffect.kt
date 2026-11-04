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
        val motion = TransitionEffects.motionAt(presentationTimeUs / 1000L,
            durationMs, incomingKind, incomingMs, outgoingKind, outgoingMs)
        return Matrix().apply {
            setScale(motion.scale, motion.scale)
            postTranslate(motion.panX, 0f)
        }
    }
}
