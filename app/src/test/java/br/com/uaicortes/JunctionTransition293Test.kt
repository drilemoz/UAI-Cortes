package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class JunctionTransition293Test {
    private fun clip(id: String, start: Long = 0L, end: Long = 1000L, source: Long = 1000L) =
        Clip(id = id, uri = "file:///$id.mp4", title = id,
            sourceDurationMs = source, startMs = start, endMs = end)

    @Test fun usesRealSourceMarginsWithoutChangingProjectClock() {
        val a = clip("a", 0L, 2000L, 2500L)
        val b = clip("b", 400L, 2000L, 2500L)
        val subtitle = SubtitleCue(startMs = 2010L, endMs = 2860L, text = "Fala")
        val audio = AudioTrack("file:///voice.wav", "Voz", 1960L, durationMs = 800L)
        val p = VideoProject(clips = listOf(a, b), subtitles = listOf(subtitle), audioTracks = listOf(audio))
        val updated = TransitionBatch.apply(p, 0, JunctionTransitionPlan.DISSOLVE, 300L, false)
        val join = JunctionTransitionPlan.project(updated).single()
        assertEquals(2000L, join.cutMs)
        assertEquals(1850L, join.incoming.timelineStartMs)
        assertEquals(250L, join.incoming.sourceStartMs)
        assertEquals(2000L, join.outgoing.sourceStartMs)
        assertFalse(join.usesHold)
        assertEquals(p, updated.copy(transitions = emptyList()))
        assertEquals(3600L, updated.durationMs)
    }

    @Test fun fullLengthClipsUseHeldEdgesInsteadOfConsumingTheVisibleTimeline() {
        val p = VideoProject(clips = listOf(clip("a"), clip("b")))
        val join = JunctionTransitionPlan.project(
            TransitionBatch.apply(p, 0, JunctionTransitionPlan.PUSH_LEFT, 250L, false)).single()
        assertTrue(join.incoming.heldFrame)
        assertTrue(join.outgoing.heldFrame)
        assertEquals(0L, join.incoming.sourceStartMs)
        assertEquals(999L, join.outgoing.sourceStartMs)
        assertEquals(875L, join.startMs)
        assertEquals(1125L, join.endMs)
    }

    @Test fun dissolveAndPushMeetInTheMiddleAndDisableOutsideTheWindow() {
        val a = clip("a")
        val b = clip("b")
        val dissolve = requireNotNull(JunctionTransitionPlan.boundary(a, b, 1000L,
            JunctionTransitionPlan.DISSOLVE, 300L))
        fun mainAlpha(us: Long) = JunctionTransitionPlan.placement(listOf(dissolve), us).mainAlpha
        assertEquals(1f, mainAlpha(849_000L), 0f)
        assertEquals(.5f, mainAlpha(1_000_000L), .0001f)
        assertEquals(1f, mainAlpha(1_150_000L), 0f)
        for ((kind, direction) in listOf(JunctionTransitionPlan.PUSH_LEFT to 1f,
            JunctionTransitionPlan.PUSH_RIGHT to -1f)) {
            val join = dissolve.copy(kind = kind)
            val before = JunctionTransitionPlan.placement(listOf(join), 999_999L)
            val after = JunctionTransitionPlan.placement(listOf(join), 1_000_000L)
            assertEquals(-direction, before.mainX, .0001f)
            assertEquals(direction, before.supportX, .0001f)
            assertEquals(before.mainX, after.supportX, .0001f)
            assertEquals(before.supportX, after.mainX, .0001f)
            assertFalse(JunctionTransitionPlan.placement(listOf(join), 1_150_000L).supportVisible)
        }
    }

    @Test fun neighboringWindowsDoNotOverlapEvenOnShortClips() {
        val clips = listOf(clip("a"), clip("b", 0L, 200L, 200L), clip("c"))
        val p = TransitionBatch.apply(VideoProject(clips = clips), 0, JunctionTransitionPlan.DISSOLVE, 1000L, true)
        val windows = JunctionTransitionPlan.project(p)
        assertEquals(2, windows.size)
        assertEquals(200L, windows.first().durationMs)
        assertEquals(windows[0].endMs, windows[1].startMs)
        assertNull(JunctionTransitionPlan.boundary(clips[0], clip("d", 0L, 60L, 60L),
            1000L, JunctionTransitionPlan.PUSH_LEFT, 300L))
    }

    @Test fun previewCropsToTheSelectedCutAndShiftsOnlyItsPrivateCopy() {
        val a = clip("a", 800L, 4600L, 5000L)
        val b = clip("b", 400L, 3900L, 4000L)
        val caption = SubtitleCue(startMs = 3770L, endMs = 4200L, text = "Na junção")
        val pip = Overlay("file:///pip.mp4", false, 3700L, startAtMs = 1200L, sourceStartMs = 100L)
        val p = VideoProject(clips = listOf(a, b), subtitles = listOf(caption), overlays = listOf(pip))
        val job = requireNotNull(JunctionPreviewProject.prepare(p, 0, JunctionTransitionPlan.DISSOLVE, 300L))
        assertEquals(2200L, job.project.durationMs)
        assertEquals(1100L, job.cutMs)
        assertEquals(3500L, job.project.clips.first().startMs)
        assertEquals(1070L, job.project.subtitles.single().startMs)
        assertEquals(1600L, job.project.overlays.single().sourceStartMs)
        assertTrue(job.project.overlays.single().muted)
        assertTrue(p.transitions.isEmpty())
        assertEquals(7300L, p.durationMs)
        assertFalse(JunctionTransitionPlan.project(job.project).single().usesHold)
    }
}
