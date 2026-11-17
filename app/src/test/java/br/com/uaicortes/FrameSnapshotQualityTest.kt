package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameSnapshotQualityTest {
    @Test fun snapshotTierNeverLosesDetailAndAlwaysHasABitrate() {
        assertEquals(480, FrameSnapshotTiming.snapshotTier(360))
        assertEquals(480, FrameSnapshotTiming.snapshotTier(480))
        assertEquals(720, FrameSnapshotTiming.snapshotTier(540))
        assertEquals(720, FrameSnapshotTiming.snapshotTier(576))
        assertEquals(1080, FrameSnapshotTiming.snapshotTier(900))
        assertEquals(1080, FrameSnapshotTiming.snapshotTier(2160))
        for (side in listOf(360, 480, 540, 576, 720, 900, 1080, 1440, 2160)) {
            VideoExportQuality.videoBitrateBps(FrameSnapshotTiming.snapshotTier(side), ExportBitratePreset.MAXIMUM)
        }
    }
}
