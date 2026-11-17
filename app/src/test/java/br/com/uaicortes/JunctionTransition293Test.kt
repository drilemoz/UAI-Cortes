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

    @Test fun audioBackedSupportUsesTheAdjacentStillWithNoTimingOrAudioChange() {
        val a = clip("a", 100L, 1200L, 2200L)
        val b = clip("b", 400L, 1500L, 2100L)
        val cut = a.durationMs
        val join = requireNotNull(JunctionTransitionPlan.boundary(a, b, cut,
            JunctionTransitionPlan.DISSOLVE, 300L))
        assertFalse(join.usesHold)
        val protected = JunctionTransitionPlan.holdAudioBackedImages(listOf(join)) { it.id == a.id }.single()
        assertEquals(join.incoming, protected.incoming)
        assertTrue(protected.outgoing.heldFrame)
        assertEquals(a.endMs - 1L, protected.outgoing.sourceStartMs)
        assertEquals(join.outgoing.timelineStartMs, protected.outgoing.timelineStartMs)
        assertEquals(join.outgoing.lengthMs, protected.outgoing.lengthMs)
        assertEquals(a, protected.outgoing.clip)
        val both = JunctionTransitionPlan.holdAudioBackedImages(listOf(join)) { true }.single()
        assertEquals(b.startMs, both.incoming.sourceStartMs)
        assertTrue(both.incoming.heldFrame)
        assertEquals(join.cutMs, both.cutMs)
        assertEquals(join.startMs, both.startMs)
        assertEquals(join.endMs, both.endMs)
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

    @Test fun planSkipsLegacyEffectWithoutResettingThePositionOfTheNextJoin() {
        val a = clip("a")
        val b = clip("b")
        val c = clip("c")
        val project = VideoProject(clips = listOf(a, b, c), transitions = listOf(
            ClipTransition(a.id, b.id, TransitionEffects.BLACK, 500L),
            ClipTransition(b.id, c.id, JunctionTransitionPlan.DISSOLVE, 300L)))
        val join = JunctionTransitionPlan.project(project).single()
        assertEquals(JunctionTransitionPlan.DISSOLVE, join.kind)
        assertEquals(2000L, join.cutMs)
        assertEquals(300L, join.durationMs)
    }

    @Test fun previewLoopsTheSelectedCutWithoutRebasingAudioOrCaptionTimes() {
        val a = clip("a", 800L, 4600L, 5000L)
        val b = clip("b", 400L, 3900L, 4000L)
        val caption = SubtitleCue(startMs = 3770L, endMs = 4200L, text = "Na junção",
            wordTimings = listOf(SubtitleWordTiming(3770L, 3880L), SubtitleWordTiming(3900L, 4200L)))
        val pip = Overlay("file:///pip.mp4", false, 3700L, startAtMs = 1200L, sourceStartMs = 100L)
        val voice = AudioTrack("file:///voice.wav", "Voz", startMs = 3760L, durationMs = 500L)
        val music = Music("file:///music.wav", "Tema", sourceDurationMs = 2000L, repeat = true)
        val p = VideoProject(clips = listOf(a, b), subtitles = listOf(caption), overlays = listOf(pip),
            audioTracks = listOf(voice), music = music)
        val job = requireNotNull(JunctionPreviewProject.prepare(p, 0, JunctionTransitionPlan.DISSOLVE, 300L))
        assertEquals(3800L, job.cutMs)
        assertEquals(2700L, job.playbackStartMs)
        assertEquals(4900L, job.playbackEndMs)
        assertEquals(p, job.project.copy(transitions = emptyList()))
        assertEquals(7300L, job.project.durationMs)
        assertFalse(JunctionTransitionPlan.project(job.project).single().usesHold)
    }
}
