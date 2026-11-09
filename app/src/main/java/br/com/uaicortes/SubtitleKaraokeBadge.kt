package br.com.uaicortes

import androidx.compose.foundation.drawBehind
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult

/** Posição da tarja calculada no layout real, inclusive texto centralizado e quebras.
 * Renderização local; não mexe no texto, timing ou tamanho da fonte.
 */
internal fun Modifier.subtitleKaraokeBadge(
    cue: SubtitleCue,
    frame: SubtitleAnimations.Frame,
    layout: TextLayoutResult?
): Modifier {
    if (cue.animation != SubtitleAnimations.KARAOKE_BADGE) return this
    val active = frame.highlighted ?: return this
    return drawBehind {
        val measured = layout ?: return@drawBehind
        if (active.first < 0 || active.last >= measured.layoutInput.text.length ||
            active.last < active.first) return@drawBehind
        val start = measured.getBoundingBox(active.first)
        val end = measured.getBoundingBox(active.last)
        // São palavras sem espaços: não atravessam duas linhas do layout.
        val height = (start.bottom - start.top).coerceAtLeast(1f)
        val padX = height * .12f
        val padY = height * .035f
        val left = start.left - padX
        val right = end.right + padX
        val top = minOf(start.top,end.top) - padY
        val bottom = maxOf(start.bottom,end.bottom) + padY
        if (right <= left || bottom <= top) return@drawBehind
        drawRoundRect(Color(cue.highlightColor),
            topLeft=Offset(left,top),size=Size(right-left,bottom-top),
            cornerRadius=CornerRadius(height*.14f,height*.14f))
    }
}
