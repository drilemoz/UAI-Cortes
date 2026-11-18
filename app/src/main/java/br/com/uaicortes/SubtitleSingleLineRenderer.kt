package br.com.uaicortes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import br.com.uaicortes.subtitles.core.SubtitleTextCasePolicy
import kotlin.math.abs
import kotlin.math.ceil

/** The real video previews and the burned-in MP4 share this renderer, not a design thumbnail. */
internal class SubtitleSingleLineRenderer {
    private data class FitKey(val cue: SubtitleCue, val width: Int)
    private data class InkKey(val fit: FitKey, val text: String, val word: IntRange?,
        val color: Int?, val bold: Boolean, val underline: Boolean)
    private var previousFit: FitKey? = null
    private var previousFont = 1f
    private var previousInk: InkKey? = null
    private var previousBitmap: Bitmap? = null

    fun render(cue: SubtitleCue, frame: SubtitleAnimations.Frame, frameWidth: Int): Bitmap? {
        val shown = SubtitleSingleLinePolicy.display(frame.text)
        if (shown.isBlank() || frameWidth < 16) return null
        val fitKey = FitKey(cue, frameWidth)
        val key = InkKey(fitKey, shown, frame.highlighted, frame.highlightColorOverride,
            frame.boldActiveWord, frame.underlineActiveWord)
        if (key == previousInk) return previousBitmap

        val available = SubtitleFrameTypography.allowedTextWidth(frameWidth)
        val nominal = SubtitleFrameTypography.fontPixelsForFrame(cue.sizeSp, frameWidth)
        if (fitKey != previousFit) {
            val stableText = SubtitleSingleLinePolicy.display(SubtitleTextCasePolicy.display(cue.text, cue.textCase))
            val measure = paint(cue, nominal, forceBold = cue.animation == SubtitleAnimations.KARAOKE_BOLD)
            // Match the longest animation state to avoid changing size when the active word changes.
            val stableWidth = if (cue.animation == SubtitleAnimations.SINGLE_WORD)
                WORDS.findAll(stableText).maxOfOrNull { Layout.getDesiredWidth(it.value, measure) } ?: 0f
                else Layout.getDesiredWidth(stableText, measure)
            previousFont = nominal * SubtitleSingleLinePolicy.fontFit(stableWidth + nominal * .72f,
                (available - 6).toFloat())
            previousFit = fitKey
        }
        val selected = frame.highlighted?.takeIf { it.first >= 0 && it.last < shown.length && !it.isEmpty() }
        val ink = SpannableString(shown)
        if (selected != null && frame.boldActiveWord)
            ink.setSpan(StyleSpan(Typeface.BOLD), selected.first, selected.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val inkPaint = paint(cue, previousFont)
        val actualWidth = Layout.getDesiredWidth(ink, inkPaint)
        inkPaint.textSize *= SubtitleSingleLinePolicy.fontFit(actualWidth + inkPaint.textSize * .72f,
            (available - 6).toFloat())
        val size = inkPaint.textSize
        val width = ceil(Layout.getDesiredWidth(ink, inkPaint)).toInt().coerceAtLeast(1) + 2
        val pad = ceil(size * .34f).toInt() + 1
        val alignment = when (SubtitleStyles.normalizeAlignment(cue.textAlignment)) {
            SubtitleStyles.ALIGN_LEFT -> Layout.Alignment.ALIGN_NORMAL
            SubtitleStyles.ALIGN_RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }
        val outline = layout(ink, inkPaint, width, alignment)
        val fillText = SpannableString(ink)
        val badgeMode = cue.animation == SubtitleAnimations.KARAOKE_BADGE
        SubtitleWordEffects.colors(cue.copy(text = shown, textCase = "original"), shown,
            if (badgeMode) null else selected, frame.highlightColorOverride ?: cue.highlightColor)
            .forEach { colored ->
                fillText.setSpan(ForegroundColorSpan(colored.color), colored.range.first,
                    colored.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        if (selected != null) {
            if (badgeMode) fillText.setSpan(ForegroundColorSpan(Color.WHITE),
                selected.first, selected.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (frame.underlineActiveWord) fillText.setSpan(UnderlineSpan(),
                selected.first, selected.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val fill = layout(fillText, TextPaint(inkPaint).apply { color = cue.color }, width, alignment)
        val bitmap = Bitmap.createBitmap(width + pad * 2, fill.height + pad * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (Color.alpha(cue.backgroundColor) != 0) {
            rectPaint.color = cue.backgroundColor
            val r = size * .15f
            canvas.drawRoundRect(RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()), r, r, rectPaint)
        }
        canvas.translate(pad.toFloat(), pad.toFloat())
        if (badgeMode && selected != null) {
            val path = android.graphics.Path()
            outline.getSelectionPath(selected.first, selected.last + 1, path)
            val bounds = RectF()
            path.computeBounds(bounds, true)
            if (!bounds.isEmpty) {
                val heavy = cue.fontFamily == SubtitleStyles.FONT_HEAVY
                val height = abs(bounds.height())
                val radius = height * if (heavy) .19f else .14f
                bounds.inset(-height * if (heavy) .18f else .12f, -height * if (heavy) .055f else .035f)
                rectPaint.color = cue.highlightColor
                canvas.drawRoundRect(bounds, radius, radius, rectPaint)
            }
        }
        if (cue.shadowOffsetDp > 0) {
            val shadow = SubtitleCaptionInk.shadowOffset(cue.shadowOffsetDp, size)
            inkPaint.color = cue.shadowColor
            canvas.save()
            canvas.translate(shadow, shadow)
            outline.draw(canvas)
            canvas.restore()
        }
        if (cue.outlineWidth > 0) {
            inkPaint.color = cue.outlineColor
            inkPaint.strokeJoin = Paint.Join.ROUND
            inkPaint.strokeWidth = SubtitleCaptionInk.outlineRadius(cue.outlineWidth, size) * 2
            inkPaint.style = Paint.Style.STROKE
            outline.draw(canvas)
        }
        fill.draw(canvas)
        // Never mutate the previous bitmap: Media3 caches its GPU texture by identity.
        previousInk = key
        previousBitmap = bitmap
        return bitmap
    }

    private fun layout(text: CharSequence, paint: TextPaint, width: Int,
        alignment: Layout.Alignment): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setIncludePad(true)
            .setAlignment(alignment)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .setMaxLines(1)
            .build()

    private fun paint(cue: SubtitleCue, size: Float, forceBold: Boolean = false): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            textSize = size
            val bold = cue.bold || forceBold
            typeface = Typeface.create(SubtitleStyles.androidFamily(cue.fontFamily), when {
                bold && cue.italic -> Typeface.BOLD_ITALIC
                bold -> Typeface.BOLD
                cue.italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            })
        }

    private companion object {
        val WORDS = Regex("\\S+")
    }
}
