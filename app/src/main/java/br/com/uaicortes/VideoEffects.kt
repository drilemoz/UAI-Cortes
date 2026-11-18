package br.com.uaicortes

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.TypefaceSpan
import android.text.style.LineBackgroundSpan
import android.text.style.StyleSpan
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.UnderlineSpan
import android.text.style.AlignmentSpan
import android.text.style.LineHeightSpan
import android.text.StaticLayout
import androidx.media3.common.util.Size
import androidx.annotation.OptIn
import android.text.Layout
import android.text.TextPaint
import android.graphics.Paint.FontMetricsInt
import android.text.Spanned
import kotlin.math.roundToInt
import kotlin.math.ceil
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import kotlin.math.min

/** Desenha o contorno antes do preenchimento, linha a linha, preservando quebras
 * e o layout principal do TextOverlay. Não altera a string nem seus tempos. */
@OptIn(UnstableApi::class)
class SubtitleOutlineSpan(
    private val outlineColor: Int,
    private val widthDp: Int,
    private val family: String,
    private val bold: Boolean,
    private val relativeSize: Float,
    private val alignment: String = SubtitleStyles.ALIGN_CENTER,
    private val italic: Boolean = false,
    private val karaoke: Boolean = false
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
        // Tamanho efetivo após o ajuste de largura do TextOverlay.
        outline.textSize = TextOverlay.TEXT_SIZE_PIXELS * relativeSize
        outline.strokeWidth = if (karaoke) 2f * SubtitleCaptionInk.outlineRadius(widthDp, outline.textSize)
            else widthDp * ((paint as? TextPaint)?.density ?: 1f) * 2f
        val systemFamily = SubtitleStyles.androidFamily(family)
        outline.typeface = Typeface.create(systemFamily, when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        })
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
@OptIn(UnstableApi::class)
class SubtitleShadowSpan(
    private val shadowColor: Int, private val offsetDp: Int,
    private val family: String, private val bold: Boolean,
    private val relativeSize: Float, private val alignment: String,
    private val italic: Boolean = false,
    private val karaoke: Boolean = false
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
        textPaint.textSize = TextOverlay.TEXT_SIZE_PIXELS * relativeSize
        textPaint.typeface = Typeface.create(SubtitleStyles.androidFamily(family), when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        })
        val textWidth = textPaint.measureText(text, start, visibleEnd)
        val x = when (SubtitleStyles.normalizeAlignment(alignment)) {
            SubtitleStyles.ALIGN_LEFT -> left.toFloat()
            SubtitleStyles.ALIGN_RIGHT -> right - textWidth
            else -> (left + right - textWidth) / 2f
        }
        val px = if (karaoke) SubtitleCaptionInk.shadowOffset(offsetDp, textPaint.textSize)
            else offsetDp * density
        canvas.drawText(text, start, visibleEnd, x + px, baseline + px, textPaint)
    }
}

/** Palavra ativa em caixa rosa arredondada, atrás do contorno preto.
 * Aplica o mesmo texto/posicionamento da linha que o SubtitleOutlineSpan.
 * Não altera as larguras do StaticLayout nem depende de bitmap/fonte externa.
 */
@OptIn(UnstableApi::class)
class SubtitleActiveWordBadgeSpan(
    private val startIndex: Int, private val endExclusive: Int,
    private val background: Int, private val family: String,
    private val bold: Boolean, private val italic: Boolean,
    private val relativeSize: Float, private val alignment: String
) : LineBackgroundSpan {
    override fun drawBackground(canvas: Canvas, paint: Paint, left: Int, right: Int,
        top: Int, baseline: Int, bottom: Int, text: CharSequence,
        start: Int, end: Int, lineNumber: Int) {
        val from = maxOf(start, startIndex)
        val to = minOf(end, endExclusive)
        if (from >= to) return
        var lineEnd = end
        while(lineEnd>start && (text[lineEnd-1]=='\n' || text[lineEnd-1]=='\r')) lineEnd--
        if (lineEnd <= start) return
        val font = Paint(paint).apply {
            isAntiAlias=true
            textSize=TextOverlay.TEXT_SIZE_PIXELS*relativeSize
            typeface=Typeface.create(SubtitleStyles.androidFamily(family),when {
                bold && italic->Typeface.BOLD_ITALIC
                bold->Typeface.BOLD
                italic->Typeface.ITALIC
                else->Typeface.NORMAL
            })
        }
        val lineWidth=font.measureText(text,start,lineEnd)
        val lineX=when(SubtitleStyles.normalizeAlignment(alignment)) {
            SubtitleStyles.ALIGN_LEFT->left.toFloat()
            SubtitleStyles.ALIGN_RIGHT->right-lineWidth
            else->(left+right-lineWidth)/2f
        }
        val before=font.measureText(text,start,from)
        val highlightedWidth=font.measureText(text,from,to)
        if (highlightedWidth<=0f) return
        val fm=font.fontMetrics
        val height=(fm.descent-fm.ascent).coerceAtLeast(1f)
        val heavy = family == SubtitleStyles.FONT_HEAVY
        val padX = height * if (heavy) .18f else .12f
        val padY = height * if (heavy) .055f else .035f
        val radius = height * if (heavy) .19f else .14f
        val badge=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=background; style=Paint.Style.FILL }
        canvas.drawRoundRect(lineX+before-padX,baseline+fm.ascent-padY,
            lineX+before+highlightedWidth+padX,baseline+fm.descent+padY,
            radius,radius,badge)
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

/** Matriz de transição por clipe. A lógica é compartilhada com prévias Compose. */
@UnstableApi
class ClipColorTransitionEffect(
    private val clipDurationMs: Long,
    private val incomingKind: String, private val incomingMs: Long,
    private val outgoingKind: String, private val outgoingMs: Long
) : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray =
        TransitionEffects.matrixAt(presentationTimeUs.coerceAtLeast(0L)/1000L,
            clipDurationMs, incomingKind, incomingMs, outgoingKind, outgoingMs)
}

/**
 * Cue and frame times are both on the exported project's timeline. Media3 already applies
 * the accumulated media-item offset before invoking the effects, including per-clip effects.
 */
@UnstableApi
class TimedSubtitleOverlay(
    private val cues: List<SubtitleCue>
) : TextOverlay() {
    // Dimensões fornecidas pelo compositor para cada vídeo (inclusive mudanças de formato).
    private var videoWidth = 720
    private var cachedBitmap: Bitmap? = null
    private var cachedKey: String? = null
    private val emptyBitmap: Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

    override fun configure(videoSize: Size) {
        super.configure(videoSize)
        if (videoWidth != videoSize.width) {
            videoWidth = SubtitleExportSizing.safeFrameWidth(videoSize.width)
            cachedKey = null
        }
    }

    /** Layout limitado ao quadro com quebra automática em palavras e margem lateral.
     * TextOverlay padrão cria StaticLayout com a largura TOTAL da string, mesmo
     * quando excede o vídeo. Assim o MP4 corta a frase no meio.
     */
    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timelineMs = presentationTimeUs / 1000L
        val active = cueAt(timelineMs)
        val frame = active?.let { SubtitleAnimations.frame(it, timelineMs) }
        if (!SubtitleOverlayTextSafety.isVisible(frame?.text.orEmpty())) return emptyBitmap
        // Inclui o estilo animado e a aparência na chave: evita reutilizar bitmap desatualizado.
        val contentKey = "${active?.hashCode()}:${frame?.text}:${frame?.highlighted}:" +
            "${frame?.highlightColorOverride}:${frame?.boldActiveWord}:" +
            "${frame?.underlineActiveWord}:$videoWidth"
        if (cachedKey == contentKey) cachedBitmap?.let { return it }
        val styled = getText(presentationTimeUs)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = TextOverlay.TEXT_SIZE_PIXELS.toFloat() }
        val width = SubtitleExportSizing.textBoxWidth(videoWidth)
        val layout = StaticLayout.Builder.obtain(styled, 0, styled.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(true)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build()
        val longestLine = (0 until layout.lineCount).maxOfOrNull { layout.getLineWidth(it) } ?: 0f
        val badgePadding = if(active?.animation==SubtitleAnimations.KARAOKE_BADGE)
            (longestLine*.02f).coerceAtLeast(10f) else 8f
        val tightWidth = ceil(longestLine + badgePadding).toInt().coerceIn(2, width)
        // O bloco segue a largura real da frase, sem uma caixa 90% larga para textos curtos.
        val finalLayout = if (tightWidth < width) StaticLayout.Builder.obtain(
            styled, 0, styled.length, paint, tightWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(true)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build() else layout
        // Sempre um Bitmap NOVO quando o conteúdo muda. Redesenhar a mesma instância
        // pode deixar a textura do overlay desatualizada no GL (o Media3 compara a
        // referência do Bitmap), causando quadros com texto antigo ("piscada").
        // Nunca reciclar o anterior: o Media3 ainda pode estar usando-o.
        val edgeInset = if (active?.animation == SubtitleAnimations.KARAOKE_BADGE &&
            active.fontFamily == SubtitleStyles.FONT_HEAVY)
            ceil((longestLine * .04f).coerceIn(16f, 56f)).toInt() else 0
        val target = Bitmap.createBitmap(tightWidth + 2 * edgeInset,
            finalLayout.height.coerceAtLeast(2), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(target)
        canvas.translate(edgeInset.toFloat(), 0f)
        finalLayout.draw(canvas)
        cachedBitmap = target
        cachedKey = contentKey
        return target
    }

    private fun cueAt(timelineMs: Long): SubtitleCue? = SubtitlePreviewPolicy.firstAt(cues, timelineMs)

    override fun getText(presentationTimeUs: Long): SpannableString {
        val timelineMs = presentationTimeUs / 1000L
        val active = cueAt(timelineMs)
        val frame = active?.let { SubtitleAnimations.frame(it, timelineMs) }
        // Media3 TextOverlay.getBitmap mede o texto e cria Bitmap(width, height).
        // Retornar "" provoca width=0 entre legendas e no primeiro frame do typewriter.
        // O NBSP garante dimensões válidas; quando inativo, nunca desenhamos esse caractere.
        val rendered = frame?.text.orEmpty()
        val text = SpannableString(SubtitleOverlayTextSafety.bitmapSafeText(rendered))
        if (!SubtitleOverlayTextSafety.isVisible(rendered)) {
            text.setSpan(ForegroundColorSpan(Color.TRANSPARENT), 0, text.length, 0)
            return text
        }
        if (active != null && frame != null) {
            text.setSpan(ForegroundColorSpan(active.color), 0, text.length, 0)
            val badgeMode=active.animation==SubtitleAnimations.KARAOKE_BADGE
            SubtitleWordEffects.colors(active.copy(text=rendered, textCase="original"),rendered,
                if(badgeMode) null else frame.highlighted,
                frame.highlightColorOverride ?: active.highlightColor).forEach { colored ->
                text.setSpan(ForegroundColorSpan(colored.color),colored.range.first,
                    colored.range.last+1,0)
            }
            if(badgeMode) frame.highlighted?.takeIf {
                it.first>=0 && it.last<text.length
            }?.let { selected ->
                text.setSpan(ForegroundColorSpan(Color.WHITE),selected.first,selected.last+1,0)
            }
            if (Color.alpha(active.backgroundColor) > 0)
                text.setSpan(BackgroundColorSpan(active.backgroundColor), 0, text.length, 0)
            if (active.bold || active.italic) text.setSpan(StyleSpan(when {
                active.bold && active.italic -> Typeface.BOLD_ITALIC
                active.bold -> Typeface.BOLD
                else -> Typeface.ITALIC
            }), 0, text.length, 0)
            val family = SubtitleStyles.androidFamily(active.fontFamily)
            text.setSpan(TypefaceSpan(family), 0, text.length, 0)
            frame.highlighted?.takeIf { it.first >= 0 && it.last < text.length }?.let { selected ->
                if (frame.boldActiveWord) text.setSpan(StyleSpan(if(active.italic)
                    Typeface.BOLD_ITALIC else Typeface.BOLD),selected.first,selected.last+1,0)
                if (frame.underlineActiveWord) text.setSpan(UnderlineSpan(),
                    selected.first,selected.last+1,0)
            }
            val fontPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = TextOverlay.TEXT_SIZE_PIXELS.toFloat()
                typeface = Typeface.create(family, when {
                    active.bold && active.italic -> Typeface.BOLD_ITALIC
                    active.bold -> Typeface.BOLD
                    active.italic -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                })
            }
            val longestWord = rendered.split(Regex("\\s+")).maxOfOrNull { fontPaint.measureText(it) } ?: 0f
            val totalWidth = rendered.lines().sumOf { fontPaint.measureText(it).toDouble() }.toFloat()
            val relativeFontSize = SubtitleExportSizing.relativeFontSize(active.sizeSp, videoWidth,
                longestWord, totalWidth)
            text.setSpan(RelativeSizeSpan(relativeFontSize), 0, text.length, 0)
            val layoutAlign = when (SubtitleStyles.normalizeAlignment(active.textAlignment)) {
                SubtitleStyles.ALIGN_LEFT -> Layout.Alignment.ALIGN_NORMAL
                SubtitleStyles.ALIGN_RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                else -> Layout.Alignment.ALIGN_CENTER
            }
            text.setSpan(AlignmentSpan.Standard(layoutAlign), 0, text.length, Spanned.SPAN_PARAGRAPH)
            if (active.lineSpacingPercent > 100)
                text.setSpan(SubtitleLineSpacingSpan(active.lineSpacingPercent), 0, text.length, Spanned.SPAN_PARAGRAPH)
            if (badgeMode) frame.highlighted?.takeIf {
                it.first>=0 && it.last<text.length
            }?.let { selected ->
                text.setSpan(SubtitleActiveWordBadgeSpan(selected.first,selected.last+1,
                    active.highlightColor,active.fontFamily,active.bold,active.italic,
                    relativeFontSize,active.textAlignment),0,text.length,0)
            }
            val karaoke = SubtitleAnimations.isKaraoke(active.animation)
            if (active.shadowOffsetDp > 0)
                text.setSpan(SubtitleShadowSpan(active.shadowColor, active.shadowOffsetDp,
                    active.fontFamily, active.bold, relativeFontSize, active.textAlignment,
                    active.italic, karaoke),0,text.length,0)
            if (active.outlineWidth > 0)
                text.setSpan(SubtitleOutlineSpan(active.outlineColor,active.outlineWidth,
                    active.fontFamily,active.bold,relativeFontSize,
                    active.textAlignment,active.italic,karaoke),0,text.length,0)
        }
        return text
    }
    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val timelineMs = presentationTimeUs / 1000L
        val active = cueAt(timelineMs)
        val frame = active?.let { SubtitleAnimations.frame(it, timelineMs) }
        val textWidth = if (active != null && SubtitleOverlayTextSafety.isVisible(frame?.text.orEmpty()))
            getBitmap(presentationTimeUs).width else 2
        val centerX = SubtitleExportSizing.centerX(active?.x ?: .5f, textWidth,
            videoWidth, frame?.scale ?: 1f)
        return StaticOverlaySettings.Builder()
            .setBackgroundFrameAnchor(((centerX-.5f)*2f + (frame?.slideX ?: 0f)*2f).coerceIn(-1f,1f),
                (1f - (active?.y ?: .85f)*2f - (frame?.slideY ?: 0f)*2f).coerceIn(-1f,1f))
            .setOverlayFrameAnchor(0f,-1f)
            .setScale(frame?.scale ?: 1f,frame?.scale ?: 1f)
            .setAlphaScale(if (active == null || !SubtitleOverlayTextSafety.isVisible(frame?.text.orEmpty()))
                0f else (frame?.alpha ?: 1f))
            .build()
    }
}
