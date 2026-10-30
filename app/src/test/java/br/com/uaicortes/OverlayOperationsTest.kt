package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class OverlayOperationsTest {
    private val video = Clip(id="video", uri="video", title="Vídeo", sourceDurationMs=15000)
    private fun p(overlay: List<Overlay> = emptyList()) = VideoProject(clips=listOf(video), overlays=overlay)

    @Test fun insertAtCursorThenSequentialAndRequiresConfirmationWhenFullTime() {
        val first = OverlayOperations.insert(p(), "image", true, 5000L, "", false, 2500L)
            as OverlayInsertion.Proposed
        assertEquals(2500L, first.requested.single().startAtMs)
        val second = OverlayOperations.insert(p(first.requested), "next", true, 5000L, "", false, 0L)
            as OverlayInsertion.Proposed
        assertEquals(7500L, second.requested.last().startAtMs)
        val full = p(listOf(Overlay("video2", false, 15000, startAtMs=0)))
        assertEquals(OverlayInsertion.ConfirmSequential,
            OverlayOperations.insert(full, "next", true, 5000L, "", false, 0))
        assertTrue(OverlayOperations.insert(full, "next", true, 5000L, "", false, 0, true) is OverlayInsertion.Proposed)
    }

    @Test fun replacementMaintainsIdAndBlocksFifthPip() {
        val layers = (1..4).map { Overlay(uri="img$it", isImage=true, durationMs=2000, id="pip$it") }
        val project = p(layers)
        assertEquals(OverlayInsertion.LimitReached,
            OverlayOperations.insert(project, "new", true, 5000, "", false, 0))
        val replace = OverlayOperations.insert(project, "updated", true, 5000, "pip2", true, 0)
            as OverlayInsertion.Proposed
        assertEquals(4, replace.requested.size)
        assertEquals("updated", replace.requested[1].uri)
        assertEquals("pip2", replace.selectedId)
        assertEquals(OverlayInsertion.LimitReached,
            OverlayOperations.insert(project, "updated", true, 5000, "nonexistent", true, 0))
    }

    @Test fun collisionRequiresApprovalAndKeepsItAfterConfirmation() {
        val layers = listOf(
            Overlay(uri="a", isImage=true, durationMs=4000, startAtMs=0, x=.5f,y=.5f,id="a"),
            Overlay(uri="b", isImage=true, durationMs=4000, startAtMs=0, x=.5f,y=.5f,id="b"))
        val project=p(layers)
        val request = OverlayOperations.apply(project, layers, "b", 16f/9f, {16f/9f})
        assertTrue(request is OverlayUpdate.Confirm)
        assertEquals(setOf("a" to "b"), (request as OverlayUpdate.Confirm).conflicts)
        val accepted = OverlayOperations.apply(project, layers, "b", 16f/9f, {16f/9f}, "keep")
            as OverlayUpdate.Applied
        assertTrue("a" in accepted.project.overlays.last().overlapWith)
        assertEquals(emptyList<String>(), project.overlays.last().overlapWith)
    }

    @Test fun removalReturnsNewSelectionWithoutChangingOriginal() {
        val layers = listOf(Overlay("x",true,1000,id="one"), Overlay("y",true,1000,id="two"))
        val (changed, selected) = OverlayOperations.remove(p(layers), "one")
        assertEquals("two", selected)
        assertEquals(listOf("two"), changed.overlays.map { it.id })
        assertEquals(2, layers.size)
    }
}
