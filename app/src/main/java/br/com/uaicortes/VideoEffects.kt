package br.com.uaicortes

import android.graphics.Color
import android.graphics.Typeface
import android.text.style.StyleSpan
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import kotlin.math.min

/** Fade (not crossfade): fades through black/white without overlapping decoder tracks. */
@UnstableApi
class ClipFadeEffect(
    private val clipDurationMs: Long,
    private val fadeInMs: Long, private val fadeOutMs: Long,
    private val incomingWhite: Boolean, private val outgoingWhite: Boolean
) : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
        val ms = presentationTimeUs / 1000L
        val inF = if (fadeInMs > 0L) (ms.toFloat() / fadeInMs).coerceIn(0f,1f) else 1f
        val outF = if (fadeOutMs > 0L) ((clipDurationMs - ms).toFloat() / fadeOutMs).coerceIn(0f,1f) else 1f
        val factor = min(inF, outF)
        val useWhite = if (inF < outF) incomingWhite else outgoingWhite
        val bias = if (useWhite) (1f - factor) else 0f
        // 4×4 column-major matrix: rgb' = rgb * factor + bias.
        return floatArrayOf(
            factor,0f,0f,0f, 0f,factor,0f,0f, 0f,0f,factor,0f,
            bias,bias,bias,1f
        )
    }
}

/** Receives absolute cue times but Media3 calls with times relative to each source clip. */
@UnstableApi
class TimedSubtitleOverlay(
    private val cues: List<SubtitleCue>, private val clipStartInProjectMs: Long
) : TextOverlay() {
    private fun cue(us: Long): SubtitleCue? {
        val t = clipStartInProjectMs + us / 1000L
        return cues.firstOrNull { t >= it.startMs && t < it.endMs && it.text.isNotBlank() }
    }
    override fun getText(presentationTimeUs: Long): SpannableString {
        val active = cue(presentationTimeUs) ?: return SpannableString("")
        val text = SpannableString(active.text)
        if (text.isNotEmpty()) {
            text.setSpan(ForegroundColorSpan(active.color), 0, text.length, 0)
            if (Color.alpha(active.backgroundColor) > 0)
                text.setSpan(BackgroundColorSpan(active.backgroundColor), 0, text.length, 0)
            if (active.bold) text.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, 0)
            text.setSpan(RelativeSizeSpan(active.sizeSp / 22f), 0, text.length, 0)
        }
        return text
    }
    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val active = cue(presentationTimeUs)
        return StaticOverlaySettings.Builder()
            .setBackgroundFrameAnchor(((active?.x ?: .5f)-.5f)*2f, 1f - (active?.y ?: .85f) * 2f)
            .setOverlayFrameAnchor(0f, -1f)
            .setAlphaScale(if (active == null) 0f else 1f)
            .build()
    }
}
