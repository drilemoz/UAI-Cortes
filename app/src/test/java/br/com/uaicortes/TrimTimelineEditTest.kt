package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class TrimTimelineEditTest {
    private val first = Clip(id = "a", uri = "content://primeiro", title = "Vídeo",
        sourceDurationMs = 16000L)
    private val other = Clip(id = "b", uri = "content://segundo", title = "Extra",
        sourceDurationMs = 5000L)
    private val project = VideoProject(clips = listOf(first, other))

    @Test fun splitSelectsRightPartAndRestoresSourceCursor() {
        val result = TrimTimelineEdit.split(project, "a", 4000L, "direita")!!
        assertEquals(3, result.project.clips.size)
        assertEquals(0L, result.project.clips[0].startMs)
        assertEquals(4000L, result.project.clips[0].endMs)
        assertEquals(4000L, result.project.clips[1].startMs)
        assertEquals(16000L, result.project.clips[1].endMs)
        assertEquals("direita", result.selectedClipId)
        assertEquals("direita", TrimTimelineEdit.resumeClipId(result.project))
        assertEquals(4000L, TrimTimelineEdit.cursorFor(result.project, result.project.clips[1]))
        assertEquals(project.durationMs, result.project.durationMs)
        assertEquals(16000L, project.clips[0].endMs) // original não foi modificado
    }

    @Test fun splittingKeepsThePreviouslyConfiguredTransition() {
        val withFade = project.copy(transitions = listOf(
            ClipTransition(fromId = "a", toId = "b", kind = "Fade preto")))
        val split = TrimTimelineEdit.split(withFade, "a", 4000L, "right")!!.project
        assertNull(EditFeatures.boundary(split, 0))
        assertEquals("Fade preto", EditFeatures.boundary(split, 1)?.kind)
        assertEquals("right", split.transitions.single().fromId)
    }

    @Test fun splitAgainFollowsThePreviousBoundaryInSourceTime() {
        val firstCut = TrimTimelineEdit.split(project, "a", 4000L, "direita")!!.project
        val nextCut = TrimTimelineEdit.split(firstCut, "direita", 10000L, "final")!!.project
        assertEquals(listOf(4000L, 6000L, 6000L, 5000L), nextCut.clips.map { it.durationMs })
        assertEquals(10000L, TrimTimelineEdit.cursorFor(nextCut, nextCut.clips[2]))
        assertEquals("final", TrimTimelineEdit.resumeClipId(nextCut))
    }

    @Test fun removeOnlyMiddlePartAndSelectsNeighbor() {
        val cut1 = TrimTimelineEdit.split(project, "a", 4000L, "meio")!!.project
        val cut2 = TrimTimelineEdit.split(cut1, "meio", 10000L, "final")!!.project
        val result = TrimTimelineEdit.remove(cut2, "meio")!!
        assertEquals(listOf("a", "final", "b"), result.project.clips.map { it.id })
        assertEquals("final", result.selectedClipId)
        assertEquals(15000L, result.project.durationMs)
        assertEquals(10000L, TrimTimelineEdit.cursorFor(result.project, result.project.clips[1]))
    }

    @Test fun removingLastMarkedPartDoesNotLeaveDanglingMark() {
        val cut = TrimTimelineEdit.split(project, "a", 4000L, "right")!!.project
        val result = TrimTimelineEdit.remove(cut, "right")!!
        assertEquals("b", result.selectedClipId) // próximo clipe na sequência
        assertEquals("a", result.project.lastTrimCutClipId) // fronteira existe no fim do esquerdo
        assertEquals(4000L, TrimTimelineEdit.cursorFor(result.project, result.project.clips[0]))
    }

    @Test fun rejectsInvalidCutsAndDoesNotDeleteLastRemainingClip() {
        assertNull(TrimTimelineEdit.split(project, "a", 100L, "x"))
        assertNull(TrimTimelineEdit.split(project, "a", 15800L, "x"))
        assertNull(TrimTimelineEdit.split(project, "ausente", 4000L, "x"))
        assertNull(TrimTimelineEdit.split(project, "a", 4000L, "b"))
        assertNull(TrimTimelineEdit.remove(VideoProject(clips = listOf(first)), "a"))
    }

    @Test fun remembersLastCutAfterJsonRoundtripAndLegacyDefaults() {
        val modified = TrimTimelineEdit.split(project, "a", 4000L, "right")!!.project
        val restored = VideoProject.read(modified.json())
        assertEquals("right", restored.lastTrimCutClipId)
        assertEquals(4000L, restored.lastTrimCutSourceMs)
        assertEquals("right", TrimTimelineEdit.resumeClipId(restored))
        val legacy = modified.json().apply {
            remove("lastTrimCutClipId")
            remove("lastTrimCutSourceMs")
        }
        val old = VideoProject.read(legacy)
        assertNull(old.lastTrimCutClipId)
        assertNull(old.lastTrimCutSourceMs)
        assertEquals("a", TrimTimelineEdit.resumeClipId(old))
    }
}
