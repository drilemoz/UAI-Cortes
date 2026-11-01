package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class VideoExportQualityTest {
    @Test fun bitratesAreExplicitAndValid() {
        assertEquals(8_000_000, VideoExportQuality.videoBitrateBps(720))
        assertEquals(18_000_000, VideoExportQuality.videoBitrateBps(1080))
        assertEquals(192_000, VideoExportQuality.AUDIO_BITRATE_BPS)
    }

    @Test fun explicitPresetsAreOrderedAndLegacyQualityIsDefault() {
        for (side in listOf(720, 1080)) {
            val compact = VideoExportQuality.videoBitrateBps(side, ExportBitratePreset.COMPACT)
            val high = VideoExportQuality.videoBitrateBps(side, ExportBitratePreset.HIGH)
            val maximum = VideoExportQuality.videoBitrateBps(side, ExportBitratePreset.MAXIMUM)
            assertTrue(compact < high)
            assertTrue(high < maximum)
            assertEquals(high, VideoExportQuality.videoBitrateBps(side))
            assertEquals(ExportBitratePreset.HIGH, ExportBitratePreset.fromId("not-a-preset"))
            assertEquals(ExportBitratePreset.MAXIMUM, ExportBitratePreset.fromId("MAXIMUM"))
        }
    }

    @Test fun estimateUsesSelectedPresetAndNeverOverflowsOnHugeDurations() {
        val compact = VideoExportQuality.estimatedMiBPerMinute(1080, true, ExportBitratePreset.COMPACT)
        val high = VideoExportQuality.estimatedMiBPerMinute(1080, true, ExportBitratePreset.HIGH)
        val maximum = VideoExportQuality.estimatedMiBPerMinute(1080, true, ExportBitratePreset.MAXIMUM)
        assertTrue(compact < high)
        assertTrue(high < maximum)
        assertTrue(VideoExportQuality.minimumFreeBytes(120_000,1080,true,ExportBitratePreset.COMPACT) <
            VideoExportQuality.minimumFreeBytes(120_000,1080,true,ExportBitratePreset.MAXIMUM))
        assertTrue(VideoExportQuality.minimumFreeBytes(Long.MAX_VALUE, 1080, true,
            ExportBitratePreset.MAXIMUM) > 0)
    }

    @Test fun wholeProjectEstimateGrowsWithDurationAndProfile() {
        val shortClip = VideoExportQuality.estimatedMiBForDuration(7_000,1080,true,
            ExportBitratePreset.HIGH)
        val longerClip = VideoExportQuality.estimatedMiBForDuration(70_000,1080,true,
            ExportBitratePreset.HIGH)
        val maximum = VideoExportQuality.estimatedMiBForDuration(70_000,1080,true,
            ExportBitratePreset.MAXIMUM)
        assertTrue(shortClip > 0)
        assertTrue(longerClip > shortClip)
        assertTrue(maximum > longerClip)
        assertThrows(IllegalArgumentException::class.java) {
            VideoExportQuality.estimatedMiBForDuration(-1L,720,true)
        }
    }

    @Test fun aProjectLongerThan24HoursMustNotBeUnderestimated() {
        val day = 86_400_000L
        val oneDay = VideoExportQuality.estimatedMiBForDuration(day, 1080, true,
            ExportBitratePreset.MAXIMUM)
        val twoDays = VideoExportQuality.estimatedMiBForDuration(day * 2, 1080, true,
            ExportBitratePreset.MAXIMUM)
        assertTrue(twoDays > oneDay)
        val reserveDay = VideoExportQuality.minimumFreeBytes(day, 1080, true,
            ExportBitratePreset.MAXIMUM)
        val reserveTwoDays = VideoExportQuality.minimumFreeBytes(day * 2, 1080, true,
            ExportBitratePreset.MAXIMUM)
        assertTrue(reserveTwoDays > reserveDay)
    }

    @Test fun veryLongDurationIsSaturatedInsteadOfOverflowing() {
        val reserve = VideoExportQuality.minimumFreeBytes(Long.MAX_VALUE, 1080, true,
            ExportBitratePreset.MAXIMUM)
        val estimate = VideoExportQuality.estimatedMiBForDuration(Long.MAX_VALUE, 1080, true,
            ExportBitratePreset.MAXIMUM)
        assertEquals(Long.MAX_VALUE, reserve)
        assertEquals(Int.MAX_VALUE, estimate)
    }

    @Test fun oneMinuteEstimateMatchesStorageFormula() {
        for (preset in ExportBitratePreset.values()) {
            val totalBitrate = VideoExportQuality.videoBitrateBps(720, preset) +
                VideoExportQuality.AUDIO_BITRATE_BPS
            val expectedBytes = totalBitrate.toLong() * 60L / 8L
            val actual = VideoExportQuality.minimumFreeBytes(60_000L,720,true,preset)
            assertEquals(expectedBytes * 2L + VideoExportQuality.MIN_WORKING_SPACE_BYTES, actual)
        }
    }

    @Test fun keyframeIntervalIsPositiveAndBounded() {
        assertEquals(2f, VideoExportQuality.REQUESTED_KEYFRAME_INTERVAL_SECONDS, 0f)
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
