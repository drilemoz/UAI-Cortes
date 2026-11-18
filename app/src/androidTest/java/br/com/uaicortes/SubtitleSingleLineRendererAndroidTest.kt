package br.com.uaicortes

import android.graphics.Color
import androidx.annotation.OptIn
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(UnstableApi::class)
class SubtitleSingleLineRendererAndroidTest {
    @Test fun longPinkCaptionUsesOneSharedBitmapAndIgnoresOverlappingSecondCue() {
        val original = "Uai, eu quero uma legenda comprida\nsem fazer duas linhas no vídeo."
        val cue = SubtitleCue(text = original, startMs = 0L, endMs = 3000L,
            sizeSp = 20, bold = true, fontFamily = SubtitleStyles.FONT_HEAVY,
            outlineWidth = 4, highlightColor = Color.MAGENTA, animation = SubtitleAnimations.KARAOKE_BADGE)
        val preview = SubtitleSingleLineRenderer()
        val width = 360
        val at = 1500L
        val frame = SubtitleAnimations.frame(cue, at)
        val bitmap = requireNotNull(preview.render(cue, frame, width))
        val oneWord = requireNotNull(preview.render(cue.copy(text = "Uai"),
            SubtitleAnimations.Frame("Uai"), width))
        assertTrue(bitmap.width <= SubtitleFrameTypography.allowedTextWidth(width))
        assertTrue("Linha longa encolhe, sem empilhar texto", bitmap.height < oneWord.height)

        val second = cue.copy(text = "Esta legenda não deve aparecer", color = Color.GREEN)
        val export = TimedSubtitleOverlay(listOf(cue, second))
        export.configure(Size(width, 640))
        val exported = export.getBitmap(at * 1000L)
        assertTrue("A mesma frase e destaque dos dois lados", exported.sameAs(bitmap))
        val pixels = IntArray(exported.width * exported.height)
        exported.getPixels(pixels, 0, exported.width, 0, 0, exported.width, exported.height)
        assertTrue("Palavra ativa ainda tem tarja colorida", pixels.any { it == Color.MAGENTA })
        assertEquals(original, cue.text)
    }
}
