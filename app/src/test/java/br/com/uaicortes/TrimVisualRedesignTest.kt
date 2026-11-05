package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

/** Operações de corte visual sem acessar codec, rede ou arquivo original. */
class TrimVisualRedesignTest {
    private val video = Clip(id = "v1", uri = "content://video", title = "Vídeo",
        sourceDurationMs = 12000L)
    private val next = Clip(id = "v2", uri = "content://next", title = "Depois",
        sourceDurationMs = 4000L)

    @Test fun timelineMapsPixelsToSourceTimeWithoutLeavingBounds() {
        assertEquals(0L, TrimVisualMath.timeAt(-10f, 240f, 12000L))
        assertEquals(6000L, TrimVisualMath.timeAt(120f, 240f, 12000L))
        assertEquals(12000L, TrimVisualMath.timeAt(999f, 240f, 12000L))
        assertEquals(0L, TrimVisualMath.timeAt(100f, 0f, 12000L))
    }

    @Test fun handlesCannotInvertTheirOrderOrCreateTinyClips() {
        val start = TrimVisualMath.selection(1000L, 4000L, 3900L, 1, 0L, 12000L)
        assertEquals(3750L to 4000L, start)
        val end = TrimVisualMath.selection(1000L, 4000L, 800L, 2, 0L, 12000L)
        assertEquals(1000L to 1250L, end)
    }

    @Test fun removingMiddleJoinsRemainingPartsAndShiftsGlobalCues() {
        val subtitle = SubtitleCue(text = "depois", startMs = 9500L, endMs = 11000L)
        val overlay = Overlay(uri = "content://pi", isImage = true,
            durationMs = 3000L, startAtMs = 9500L)
        val project = VideoProject(clips = listOf(video, next),
            subtitles = listOf(subtitle), overlays = listOf(overlay))
        val cut = TrimRemoveRange.apply(project, "v1", 3000L, 7000L,
            firstNewId = "reserve", secondNewId = "right")!!
        assertEquals(listOf(3000L, 5000L, 4000L), cut.project.clips.map { it.durationMs })
        assertEquals("right", cut.selectedClipId)
        assertEquals(12000L, cut.project.durationMs)
        assertEquals(5500L, cut.project.subtitles.single().startMs)
        assertEquals(7000L, cut.project.subtitles.single().endMs)
        assertEquals(5500L, cut.project.overlays.single().startAtMs)
        assertEquals(12000L, video.endMs)
    }

    @Test fun removePrefixAndSuffixPreserveClipIdAndNoEmptyProject() {
        val alone = VideoProject(clips = listOf(video))
        val prefix = TrimRemoveRange.apply(alone, "v1", 0L, 2500L)!!
        assertEquals("v1", prefix.project.clips.single().id)
        assertEquals(2500L, prefix.project.clips.single().startMs)
        val suffix = TrimRemoveRange.apply(alone, "v1", 9000L, 12000L)!!
        assertEquals("v1", suffix.project.clips.single().id)
        assertEquals(9000L, suffix.project.clips.single().endMs)
        assertNull(TrimRemoveRange.apply(alone, "v1", 0L, 12000L))
    }

    @Test fun removeWholeClipIfNeighborExists() {
        val project = VideoProject(clips = listOf(video, next))
        val result = TrimRemoveRange.apply(project, "v1", 0L, 12000L)!!
        assertEquals(listOf("v2"), result.project.clips.map { it.id })
        assertEquals(4000L, result.project.durationMs)
    }

    @Test fun refusesTinyRemaindersAndInvalidIntervals() {
        val alone = VideoProject(clips = listOf(video))
        assertNull(TrimRemoveRange.apply(alone, "v1", 100L, 1000L))
        assertNull(TrimRemoveRange.apply(alone, "v1", 3000L, 11900L))
        assertNull(TrimRemoveRange.apply(alone, "v1", 3000L, 3100L))
        assertNull(TrimRemoveRange.apply(alone, "v1", -10L, 1000L))
        assertNull(TrimRemoveRange.apply(alone, "v1", 3000L, 12100L))
    }

    @Test fun preservesTheOldTransitionAtTheNewRealEnd() {
        val withTransition = VideoProject(clips = listOf(video, next),
            transitions = listOf(ClipTransition(fromId = "v1", toId = "v2", kind = "Fade preto")))
        val result = TrimRemoveRange.apply(withTransition, "v1", 2000L, 5000L,
            firstNewId = "unused", secondNewId = "right")!!.project
        assertNull(EditFeatures.boundary(result, 0))
        assertEquals("right", result.transitions.single().fromId)
        assertEquals("Fade preto", EditFeatures.boundary(result, 1)?.kind)
    }
}
