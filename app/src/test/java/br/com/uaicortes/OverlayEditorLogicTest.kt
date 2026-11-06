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
    @Test fun acceptsSmallAndManualPercentages() {
        assertEquals(.05f, OverlayEditorLogic.parsePercentage("5"))
        assertEquals(.10f, OverlayEditorLogic.parsePercentage("10%"))
        assertEquals(.15f, OverlayEditorLogic.parsePercentage(" 15 "))
        assertEquals(null, OverlayEditorLogic.parsePercentage("0"))
        assertEquals(null, OverlayEditorLogic.parsePercentage("91"))
        assertEquals(null, OverlayEditorLogic.parsePercentage("qualquer"))
        val (w, h) = OverlayEditorLogic.dimensions(.05f, 16f / 9f, 16f / 9f)
        assertTrue(w <= .051f && w >= .049f && h > 0f)
    }
    @Test fun logoCanFillWidthWithoutMandatoryMargin() {
        val (width,height)=OverlayEditorLogic.dimensions(1f,3f,9f/16f)
        assertEquals(1f,width,.0001f)
        assertTrue(height>0f && height<1f)
        assertEquals(.5f,OverlayEditorLogic.clampCenter(.02f,width),.0001f)
        val wide=OverlayEditorLogic.dimensions(1.5f,3f,9f/16f)
        assertEquals(1.5f,wide.first,.0001f)
        assertEquals(.5f,OverlayEditorLogic.clampCenter(.9f,wide.first),.0001f)
    }
    @Test fun fillModeCropsWithoutDistortingAndManualScaleIsBounded() {
        val (w,h)=OverlayEditorLogic.dimensions(1f,3f,9f/16f,OverlayEditorLogic.FILL)
        assertTrue(w>=1f && h>=1f)
        assertEquals(w / h,3f/(9f/16f),.0001f)
        assertEquals(.05f,OverlayEditorLogic.parsePercentage("5"))
        assertEquals(1f,OverlayEditorLogic.parsePercentage("100"))
        assertEquals(1.5f,OverlayEditorLogic.parsePercentage("150"))
        assertEquals(null,OverlayEditorLogic.parsePercentage("151"))
        assertEquals(OverlayEditorLogic.FIT,OverlayEditorLogic.fitMode("unknown"))
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
