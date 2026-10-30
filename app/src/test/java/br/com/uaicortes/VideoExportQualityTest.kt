package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class VideoExportQualityTest {
    @Test fun bitratesAreExplicitAndValid() {
        assertEquals(8_000_000, VideoExportQuality.videoBitrateBps(720))
        assertEquals(18_000_000, VideoExportQuality.videoBitrateBps(1080))
        assertEquals(192_000, VideoExportQuality.AUDIO_BITRATE_BPS)
    }

    @Test fun rejectsUnsupportedResolutions() {
        assertThrows(IllegalArgumentException::class.java) {
            VideoExportQuality.videoBitrateBps(480)
        }
    }

    @Test fun higherResolutionNeedsMoreBytes() {
        assertEquals(59, VideoExportQuality.estimatedMiBPerMinute(720,true))
        assertEquals(131, VideoExportQuality.estimatedMiBPerMinute(1080,true))
        assertTrue(VideoExportQuality.estimatedMiBPerMinute(1080,false) <
            VideoExportQuality.estimatedMiBPerMinute(1080,true))
        assertTrue(VideoExportQuality.MIN_WORKING_SPACE_BYTES > 64L * 1024 * 1024)
        assertTrue(VideoExportQuality.minimumFreeBytes(60_000L, 1080, true) >
            VideoExportQuality.minimumFreeBytes(60_000L, 720, true))
        assertTrue(VideoExportQuality.minimumFreeBytes(60_000L, 1080, true) >
            VideoExportQuality.estimatedMiBPerMinute(1080, true).toLong() * 1024 * 1024)
    }
}
