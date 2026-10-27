package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleExportSizingTest {
    @Test fun textBoxAlwaysWithinNinetyPercent() {
        for (width in listOf(0, 1, 360, 405, 720, 1080, 1920, 3840)) {
            val actual = SubtitleExportSizing.textBoxWidth(width)
            val safe = SubtitleExportSizing.safeFrameWidth(width)
            assertTrue(actual >= 2)
            assertTrue(actual <= (safe * .9f).toInt().coerceAtLeast(2))
        }
    }
    @Test fun fontIsProportionalToFrameAndProjectSize() {
        val a = SubtitleExportSizing.relativeFontSize(12, 405, 220f)
        val b = SubtitleExportSizing.relativeFontSize(12, 1080, 220f)
        val larger = SubtitleExportSizing.relativeFontSize(22, 1080, 220f)
        assertTrue(b > a)
        assertTrue(larger > b)
    }
    @Test fun longLineAnchorStaysInsideVideo() {
        assertEquals(.45f, SubtitleExportSizing.centerX(.1f, 972, 1080, 1f), .0001f)
        assertEquals(.55f, SubtitleExportSizing.centerX(.9f, 972, 1080, 1f), .0001f)
        assertEquals(.2f, SubtitleExportSizing.centerX(.2f, 100, 1080, 1f), .0001f)
    }
    @Test fun longWordNeverExceedsFrameAndNoNaN() {
        for (width in listOf(405, 720, 1080, 1920)) {
            for (font in listOf(12, 22, 48)) {
                val longest = 5000f
                val scale = SubtitleExportSizing.relativeFontSize(font, width, longest)
                assertTrue(scale.isFinite() && scale > 0f)
                assertTrue(longest * scale <= SubtitleExportSizing.textBoxWidth(width) + .01f)
            }
        }
    }
}
