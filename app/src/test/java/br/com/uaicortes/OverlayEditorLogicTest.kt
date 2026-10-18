package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayEditorLogicTest {
    @Test fun allPresetsRemainOnScreen() {
        val (w, h) = OverlayEditorLogic.dimensions(.36f, 16f / 9f, 16f / 9f)
        for (row in 0..2) for (col in 0..2) {
            val (x, y) = OverlayEditorLogic.preset(row, col, w, h)
            assertTrue(x - w / 2 >= 0)
            assertTrue(x + w / 2 <= 1)
            assertTrue(y - h / 2 >= 0)
            assertTrue(y + h / 2 <= 1)
        }
    }
    @Test fun tallAssetFitsWithinCanvas() {
        val (w, h) = OverlayEditorLogic.dimensions(.85f, .5f, 16f / 9f)
        assertTrue(w < .85f)
        assertTrue(h <= .91f)
    }
    @Test fun startMoveMaintainsEnd() {
        val (s, len) = OverlayEditorLogic.startChanged(9000L, 7000L, 5000L, 45000L)
        assertEquals(9000L, s)
        assertEquals(12000L, s + len)
    }
    @Test fun timeBoundsNeverBecomeNegative() {
        val (start, len) = OverlayEditorLogic.startChanged(12000L, 7000L, 5000L, 45000L)
        assertEquals(11500L, start)
        assertEquals(500L, len)
        assertEquals(500L, OverlayEditorLogic.endChanged(100L, 11500L, 45000L))
    }
}
