package br.com.uaicortes

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.TypefaceSpan
import android.text.style.LineBackgroundSpan
import android.text.style.StyleSpan
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.LineHeightSpan
import android.text.Layout
import android.text.TextPaint
import android.graphics.Paint.FontMetricsInt
import android.text.Spanned
import kotlin.math.roundToInt
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import kotlin.math.min

/** Desenha o contorno antes do preenchimento, linha a linha, preservando quebras
 * e o layout principal do TextOverlay. Não altera a string nem seus tempos. */
class SubtitleOutlineSpan(
    private val outlineColor: Int,
    private val widthDp: Int,
    private val family: String,
    private val bold: Boolean,
    private val relativeSize: Float,
    private val alignment: String = SubtitleStyles.ALIGN_CENTER
) : LineBackgroundSpan {
    override fun drawBackground(canvas: Canvas, paint: Paint, left: Int, right: Int,
        top: Int, baseline: Int, bottom: Int, text: CharSequence,
        start: Int, end: Int, lineNumber: Int) {
        if (widthDp <= 0 || start >= end) return
        var visibleEnd = end
        while (visibleEnd > start && (text[visibleEnd-1] == '\n' || text[visibleEnd-1] == '\r')) visibleEnd--
        if (visibleEnd == start) return
        val outline = Paint(paint)
        outline.color = outlineColor
        outline.style = Paint.Style.STROKE
        outline.strokeJoin = Paint.Join.ROUND
        outline.strokeWidth = widthDp * ((paint as? android.text.TextPaint)?.density ?: 1f) * 2f
        outline.textSize = paint.textSize * relativeSize
        val systemFamily = when (SubtitleStyles.normalizeFont(family)) {
            SubtitleStyles.FONT_SERIF -> "serif"
            SubtitleStyles.FONT_MONO -> "monospace"
            else -> "sans-serif"
        }
        outline.typeface = Typeface.create(systemFamily, if (bold) Typeface.BOLD else Typeface.NORMAL)
        val width = outline.measureText(text, start, visibleEnd)
        val x = when (SubtitleStyles.normalizeAlignment(alignment)) {
            SubtitleStyles.ALIGN_LEFT -> left.toFloat()
            SubtitleStyles.ALIGN_RIGHT -> right - width
            else -> (left + right - width) / 2f
        }
        canvas.drawText(text, start, visibleEnd, x, baseline.toFloat(), outline)
    }
}


/** Draw a sharp offset shadow behind a Media3 TextOverlay line.
 * Uses the same font and baseline as the outline span. No network/font assets.
 */
class SubtitleShadowSpan(
    private val shadowColor: Int, private val offsetDp: Int,
    private val family: String, private val bold: Boolean,
    private val relativeSize: Float, private val alignment: String
) : LineBackgroundSpan {
    override fun drawBackground(canvas: Canvas, paint: Paint, left: Int, right: Int,
        top: Int, baseline: Int, bottom: Int, text: CharSequence,
        start: Int, end: Int, lineNumber: Int) {
        if (offsetDp <= 0 || start >= end) return
        var visibleEnd = end
        while (visibleEnd > start && (text[visibleEnd - 1] == '\n' || text[visibleEnd - 1] == '\r')) visibleEnd--
        if (visibleEnd == start) return
        val textPaint = Paint(paint)
        textPaint.isAntiAlias = true
        textPaint.color = shadowColor
        textPaint.style = Paint.Style.FILL
        val density = (paint as? TextPaint)?.density ?: 1f
        textPaint.textSize = paint.textSize * relativeSize
        textPaint.typeface = Typeface.create(when (SubtitleStyles.normalizeFont(family)) {
            SubtitleStyles.FONT_SERIF -> "serif"
            SubtitleStyles.FONT_MONO -> "monospace"
            else -> "sans-serif"
        }, if (bold) Typeface.BOLD else Typeface.NORMAL)
        val textWidth = textPaint.measureText(text, start, visibleEnd)
        val x = when (SubtitleStyles.normalizeAlignment(alignment)) {
            SubtitleStyles.ALIGN_LEFT -> left.toFloat()
            SubtitleStyles.ALIGN_RIGHT -> right - textWidth
            else -> (left + right - textWidth) / 2f
        }
        val px = offsetDp * density
        canvas.drawText(text, start, visibleEnd, x + px, baseline + px, textPaint)
    }
}

/** Relative additional leading; 100 keeps Android layout's default line height. */
class SubtitleLineSpacingSpan(private val percent: Int) : LineHeightSpan {
    override fun chooseHeight(text: CharSequence, start: Int, end: Int, spanstartv: Int,
        v: Int, fm: FontMetricsInt) {
        val height = fm.descent - fm.ascent
        if (height <= 0) return
        val extra = (height * (SubtitleStyles.normalizeLineSpacing(percent) - 100) / 100f).roundToInt()
        fm.descent += extra
        fm.bottom += extra
    }
}

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
        val frame=SubtitleAnimations.frame(active,clipStartInProjectMs + presentationTimeUs/1000L)
        val text = SpannableString(frame.text)
        if (text.isNotEmpty()) {
            text.setSpan(ForegroundColorSpan(active.color), 0, text.length, 0)
            frame.highlighted?.let { highlighted ->
                if(highlighted.first>=0 && highlighted.last<text.length)
                    text.setSpan(ForegroundColorSpan(Color.YELLOW),highlighted.first,
                        highlighted.last+1,0)
            }
            if (Color.alpha(active.backgroundColor) > 0)
                text.setSpan(BackgroundColorSpan(active.backgroundColor), 0, text.length, 0)
            if (active.bold) text.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, 0)
            val family = when (SubtitleStyles.normalizeFont(active.fontFamily)) {
                SubtitleStyles.FONT_SERIF -> "serif"
                SubtitleStyles.FONT_MONO -> "monospace"
                else -> "sans-serif"
            }
            text.setSpan(TypefaceSpan(family), 0, text.length, 0)
            text.setSpan(RelativeSizeSpan(active.sizeSp / 22f), 0, text.length, 0)
            val layoutAlign = when (SubtitleStyles.normalizeAlignment(active.textAlignment)) {
                SubtitleStyles.ALIGN_LEFT -> Layout.Alignment.ALIGN_NORMAL
                SubtitleStyles.ALIGN_RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                else -> Layout.Alignment.ALIGN_CENTER
            }
            text.setSpan(AlignmentSpan.Standard(layoutAlign), 0, text.length, Spanned.SPAN_PARAGRAPH)
            if (active.lineSpacingPercent > 100)
                text.setSpan(SubtitleLineSpacingSpan(active.lineSpacingPercent), 0, text.length, Spanned.SPAN_PARAGRAPH)
            if (active.shadowOffsetDp > 0)
                text.setSpan(SubtitleShadowSpan(active.shadowColor, active.shadowOffsetDp,
                    active.fontFamily, active.bold, active.sizeSp / 22f, active.textAlignment),0,text.length,0)
            if (active.outlineWidth > 0)
                text.setSpan(SubtitleOutlineSpan(active.outlineColor,active.outlineWidth,
                    active.fontFamily,active.bold,active.sizeSp / 22f,
                    active.textAlignment),0,text.length,0)
        }
        return text
    }
    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val active = cue(presentationTimeUs)
        val frame=active?.let { SubtitleAnimations.frame(it,
            clipStartInProjectMs + presentationTimeUs/1000L) }
        return StaticOverlaySettings.Builder()
            .setBackgroundFrameAnchor(((active?.x ?: .5f)-.5f)*2f,
                (1f - (active?.y ?: .85f)*2f - (frame?.slideY ?: 0f)*2f).coerceIn(-1f,1f))
            .setOverlayFrameAnchor(0f,-1f)
            .setScale(frame?.scale ?: 1f,frame?.scale ?: 1f)
            .setAlphaScale(if(active==null) 0f else (frame?.alpha ?: 1f))
            .build()
    }
}
