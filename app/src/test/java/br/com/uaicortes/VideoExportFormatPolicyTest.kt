package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class VideoExportFormatPolicyTest {
    @Test fun socialDestinationsRequestExactVerticalFrames() {
        for (destination in ExportDestination.values().filter { it.isVertical }) {
            assertEquals(360 to 640, VideoExportFormatPolicy.dimensions(destination, 360))
            assertEquals(480 to 854, VideoExportFormatPolicy.dimensions(destination, 480))
            assertEquals(720 to 1280, VideoExportFormatPolicy.dimensions(destination, 720))
            assertEquals(1080 to 1920, VideoExportFormatPolicy.dimensions(destination, 1080))
            assertEquals("1080 × 1920 (9:16)", VideoExportFormatPolicy.requestedLabel(destination,1080))
            assertEquals(ExportBitratePreset.COMPACT,
                VideoExportFormatPolicy.recommendedBitrate(destination))
        }
    }

    @Test fun originalKeepsExistingGeometryAndHighQuality() {
        assertNull(VideoExportFormatPolicy.dimensions(ExportDestination.ORIGINAL,720))
        assertEquals(ExportBitratePreset.HIGH,
            VideoExportFormatPolicy.recommendedBitrate(ExportDestination.ORIGINAL))
        assertEquals(ExportDestination.ORIGINAL, ExportDestination.fromId("desconhecido"))
        assertEquals(VerticalFrameMode.FIT, VerticalFrameMode.fromId("desconhecido"))
    }

    @Test fun legacySavedFormatsStillResolveToVertical() {
        for (old in listOf("SHORTS", "REELS", "TIKTOK")) {
            assertEquals(ExportDestination.VERTICAL, ExportDestination.fromId(old))
        }
    }

    @Test fun onlySupportedResolutionsAreAccepted() {
        assertThrows(IllegalArgumentException::class.java) {
            VideoExportFormatPolicy.dimensions(ExportDestination.VERTICAL, 240)
        }
        assertThrows(IllegalArgumentException::class.java) {
            VideoExportFormatPolicy.dimensions(ExportDestination.ORIGINAL, 1440)
        }
    }

    @Test fun framingIsExplicitAndWarnsBeforeCropping() {
        assertTrue(VideoExportFormatPolicy.framingDescription(VerticalFrameMode.FIT).contains("barras"))
        assertTrue(VideoExportFormatPolicy.framingDescription(VerticalFrameMode.CENTER_CROP).contains("cortando"))
    }

    @Test fun exactPortraitReportDetectsChangedAspectEvenWhenShortSideMatches() {
        val report = ExportQualityReport(720,720,1280,5_000_000,192_000,
            10_000,300,7_000_000,"video/avc",null,
            requestedFrameWidth=720,requestedFrameHeight=1280,destinationLabel="Shorts")
        assertFalse(report.fellBackInResolution)
        assertTrue(report.lines().any { it.contains("720 × 1280") })
        assertTrue(report.lines().any { it.contains("Shorts") })
        assertTrue(report.copy(width=1280,height=720).fellBackInResolution)
        assertTrue(report.copy(width=720,height=1080).fellBackInResolution)
    }
}
