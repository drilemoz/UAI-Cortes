package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleSingleLinePolicyTest {
    @Test fun flattensImportedBreaksButRetainsOffsetsForActiveWord() {
        val original = "Uai!\r\nAção\u2028agora\t👍"
        val shown = SubtitleSingleLinePolicy.display(original)
        assertEquals("Uai!  Ação agora 👍", shown)
        assertEquals(original.length, shown.length)
        assertEquals(original.indexOf("agora"), shown.indexOf("agora"))
    }

    @Test fun longTextShrinksBelowOldMultilineFloorInsteadOfWrapping() {
        assertEquals(.2f, SubtitleSingleLinePolicy.fontFit(1000f, 200f), .0001f)
        assertEquals(1f, SubtitleSingleLinePolicy.fontFit(120f, 200f), .0001f)
    }

    @Test fun aSlideOrPulseCannotPushTheLineOutsideTheFrame() {
        val p = SubtitleSingleLinePolicy.place(.9f, .95f, .3f, .2f, 1.25f,
            290, 40, 320, 568)
        assertEquals(1f, p.bottomY, .0001f)
        assertTrue(p.scale < 1.25f)
        assertEquals(.5f, p.centerX, .0001f)
    }

    @Test fun normalPositionStaysAtTheSameAnchorAcrossResolutions() {
        val low = SubtitleSingleLinePolicy.place(.45f, .8f, 0f, 0f, 1f,
            190, 28, 360, 640)
        val high = SubtitleSingleLinePolicy.place(.45f, .8f, 0f, 0f, 1f,
            570, 84, 1080, 1920)
        assertEquals(low, high)
    }
}
