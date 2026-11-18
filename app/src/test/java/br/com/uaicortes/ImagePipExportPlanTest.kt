package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ImagePipExportPlanTest {
    private val photo = Overlay("file:///logo.png", isImage = true, startAtMs = 481, durationMs = 200)
    private val other = photo.copy(id = "other")
    private val video = photo.copy(id = "video", isImage = false)

    @Test fun timingUsesAbsoluteHalfOpenFrameWindow() {
        assertFalse(ImagePipExportPlan.contains(photo, 480_999))
        assertTrue(ImagePipExportPlan.contains(photo, 481_000))
        assertTrue(ImagePipExportPlan.contains(photo, 680_999))
        assertFalse(ImagePipExportPlan.contains(photo, 681_000))
        assertFalse(ImagePipExportPlan.contains(photo, 0))
    }

    @Test fun photoLayersLeaveCompositorOnlyWhenTheyAreAtTheTop() {
        val projectOrder = listOf(other, video, photo)
        val topFirst = ExportLayerOrder.media3TopFirst(projectOrder)
        val count = ImagePipExportPlan.timedCount(topFirst)
        assertEquals(listOf(photo), topFirst.take(count))
        assertEquals(listOf(video, other), topFirst.drop(count))
        assertEquals(2, ImagePipExportPlan.timedCount(listOf(photo, other)))
        assertEquals(0, ImagePipExportPlan.timedCount(listOf(video, photo)))
        assertEquals(0, ImagePipExportPlan.timedCount(emptyList()))
    }
}
