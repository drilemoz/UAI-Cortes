package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

/** Regressão do ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED / width and height must be > 0. */
class SubtitleOverlayTextSafetyTest {
    @Test fun inactiveTimeAndEmptyTypewriterProduceNonEmptyBitmapText() {
        for (text in listOf("", " ", "\n", "\r\n", "\t")) {
            assertFalse(SubtitleOverlayTextSafety.isVisible(text))
            assertTrue(SubtitleOverlayTextSafety.bitmapSafeText(text).isNotEmpty())
            assertEquals("\u00A0", SubtitleOverlayTextSafety.bitmapSafeText(text))
        }
    }
    @Test fun captionsKeepRealTextUnchanged() {
        for (text in listOf("Oi", "São Francisco", "texto\nna tela", "áudio 🎥")) {
            assertTrue(SubtitleOverlayTextSafety.isVisible(text))
            assertEquals(text, SubtitleOverlayTextSafety.bitmapSafeText(text))
        }
    }
    @Test fun placeholderIsSingleVisibleWidthGlyphButNeverDisplayed() {
        val sentinel=SubtitleOverlayTextSafety.bitmapSafeText("")
        assertEquals(1,sentinel.length)
        assertFalse(SubtitleOverlayTextSafety.isVisible(sentinel))
        assertEquals('\u00A0', sentinel.single())
    }
}
