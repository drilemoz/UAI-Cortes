package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class FrameCapturePlanTest {
    private val clips = listOf(FrameCaptureClip("content://a", 1000, 5000),
        FrameCaptureClip("content://b", 2000, 7000))
    @Test fun capturesWithinTrimmedOriginalClip() {
        assertEquals(FrameCaptureTarget("content://a", 1000), FrameCapturePlan.locate(clips, 0))
        assertEquals(FrameCaptureTarget("content://a", 3999), FrameCapturePlan.locate(clips, 2999))
    }
    @Test fun globalTimelineCrossesClipWithoutResettingSourceTime() {
        assertEquals(FrameCaptureTarget("content://a", 4999), FrameCapturePlan.locate(clips, 4000))
        assertEquals(FrameCaptureTarget("content://b", 2001), FrameCapturePlan.locate(clips, 4001))
        assertEquals(FrameCaptureTarget("content://b", 6999), FrameCapturePlan.locate(clips, 100000))
    }
    @Test fun invalidAndEmptySourcesDoNotCapture() {
        assertNull(FrameCapturePlan.locate(emptyList(), 10))
        assertNull(FrameCapturePlan.locate(listOf(FrameCaptureClip("", 0, 10)), 1))
        assertNull(FrameCapturePlan.locate(listOf(FrameCaptureClip("c", 20, 10)), 1))
    }
    @Test fun filenameSanitizedAndUniqueWithTimestamp() {
        val file = FrameCapturePlan.filename("Meu: vídeo / teste", 9876543, 3250)
        assertTrue(file.endsWith("_quadro_9876543_3250.png"))
        assertFalse(file.contains(':'))
        assertFalse(file.contains('/'))
    }
}
