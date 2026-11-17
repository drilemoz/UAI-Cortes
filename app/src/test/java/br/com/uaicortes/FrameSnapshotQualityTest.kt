package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameSnapshotQualityTest {
    @Test fun snapshotTierNeverLosesDetailAndAlwaysHasABitrate() {
        assertEquals(480, VideoExportQuality.snapshotTier(360))
        assertEquals(480, VideoExportQuality.snapshotTier(480))
        assertEquals(720, VideoExportQuality.snapshotTier(540))
        assertEquals(720, VideoExportQuality.snapshotTier(576))
        assertEquals(1080, VideoExportQuality.snapshotTier(900))
        assertEquals(1080, VideoExportQuality.snapshotTier(2160))
        for (side in listOf(360, 480, 540, 576, 720, 900, 1080, 1440, 2160)) {
            VideoExportQuality.videoBitrateBps(VideoExportQuality.snapshotTier(side), ExportBitratePreset.MAXIMUM)
        }
    }
}
