package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveVideoLayoutTest {
    @Test fun previewPreservesPortraitVideoRatio() {
        val frame = AdaptiveVideoLayout.inline(360f, 800f, 9f / 16f)
        assertEquals(380f, frame.height, .01f)
        assertEquals(9f / 16f, frame.width / frame.height, .0001f)
    }

    @Test fun landscapeVideoUsesTheAvailableWidth() {
        val frame = AdaptiveVideoLayout.inline(360f, 800f, 16f / 9f)
        assertEquals(360f, frame.width, .01f)
        assertEquals(202.5f, frame.height, .01f)
    }

    @Test fun squareVideoDoesNotStretch() {
        val frame = AdaptiveVideoLayout.inline(360f, 800f, 1f)
        assertEquals(360f, frame.width, .01f)
        assertEquals(360f, frame.height, .01f)
    }

    @Test fun shortScreenLimitsPortraitHeight() {
        val frame = AdaptiveVideoLayout.inline(360f, 600f, 9f / 16f)
        assertEquals(288f, frame.height, .01f)
        assertEquals(162f, frame.width, .01f)
    }

    @Test fun invalidMetadataUsesSafeRatio() {
        val frame = AdaptiveVideoLayout.fit(360f, 300f, Float.NaN)
        assertEquals(360f, frame.width, .01f)
        assertEquals(202.5f, frame.height, .01f)
        assertEquals(16f / 9f, AdaptiveVideoLayout.safeAspect(0f), .0001f)
    }

    @Test fun emptyContainerProducesEmptyFrame() {
        assertEquals(0f, AdaptiveVideoLayout.fit(0f, 300f, 1f).width, .01f)
        assertEquals(0f, AdaptiveVideoLayout.fit(300f, -5f, 1f).height, .01f)
    }

    @Test fun activeClipTracksChangingOrientation() {
        val clips = listOf(4000L, 6000L, 2000L)
        assertEquals(0, AdaptiveVideoLayout.activeClipIndex(clips, 0L))
        assertEquals(1, AdaptiveVideoLayout.activeClipIndex(clips, 4000L))
        assertEquals(1, AdaptiveVideoLayout.activeClipIndex(clips, 9999L))
        assertEquals(2, AdaptiveVideoLayout.activeClipIndex(clips, 10000L))
        assertEquals(2, AdaptiveVideoLayout.activeClipIndex(clips, 12000L))
        assertEquals(-1, AdaptiveVideoLayout.activeClipIndex(emptyList(), 0L))
    }

    @Test fun extremeRatiosStayInBounds() {
        for (ratio in listOf(.1f, .2f, 1f, 5f, 10f)) {
            val frame = AdaptiveVideoLayout.inline(360f, 800f, ratio)
            assertTrue(frame.width in 0f..360f)
            assertTrue(frame.height in 0f..380f)
            assertEquals(ratio, frame.width / frame.height, .0001f)
        }
    }
}
