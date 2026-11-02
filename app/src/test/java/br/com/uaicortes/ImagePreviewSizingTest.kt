package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagePreviewSizingTest {
    @Test fun largePortraitPreservesProportion() {
        assertEquals(1080 to 1920, ImagePreviewSizing.target(3024, 5376, 1920))
        assertEquals(720 to 1280, ImagePreviewSizing.target(3024, 5376, 1280))
    }

    @Test fun smallerSourceNeverUpscaled() {
        assertEquals(360 to 640, ImagePreviewSizing.target(360, 640, 1920))
        assertEquals(640 to 360, ImagePreviewSizing.target(640, 360, 1280))
    }

    @Test fun largeLandscapeSamplesConservatively() {
        assertEquals(2, ImagePreviewSizing.sampleSize(4032, 3024, 1280))
        assertEquals(4, ImagePreviewSizing.sampleSize(8000, 6000, 1920))
        assertEquals(1, ImagePreviewSizing.sampleSize(640, 480, 720))
    }

    @Test fun allQualityModesHaveIncreasingResolution() {
        val modes = ImagePreviewQuality.entries
        assertEquals(listOf("Rápida", "Equilibrada", "Alta"), modes.map { it.label })
        assertTrue(modes.zipWithNext().all { (a, b) -> a.maxLongSide < b.maxLongSide })
    }
}
