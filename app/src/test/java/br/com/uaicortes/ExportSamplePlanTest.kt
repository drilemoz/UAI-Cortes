package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ExportSamplePlanTest {
    val clip = Clip(uri = "v", title = "", sourceDurationMs = 12_000, startMs = 2000)

    @Test fun selectionPreservesActiveWordPipAndLoopPhase() {
        val cue = SubtitleCue(text = "um dois", startMs = 1000, endMs = 7000,
            wordTimings = listOf(SubtitleWordTiming(1000, 3000), SubtitleWordTiming(3000, 7000)))
        val pip = Overlay(uri = "pip", isImage = false, startAtMs = 1000, durationMs = 5000, sourceStartMs = 500)
        val p = VideoProject(clips = listOf(clip), subtitles = listOf(cue), overlays = listOf(pip),
            music = Music("music", ""))
        val sample = ExportSamplePlan.prepare(p, 4000, 3000) { 5000 }
        val excerpt = sample.project
        assertEquals(6000L, excerpt.clips.single().startMs)
        assertEquals(3000L, excerpt.durationMs)
        assertEquals(1, SubtitleWordEffects.singleWordIndex(excerpt.subtitles.single(), 0L))
        assertEquals(3500L, excerpt.overlays.single().sourceStartMs)
        assertEquals(2000L, excerpt.overlays.single().durationMs)
        assertEquals(listOf(4000L, 0L), excerpt.audioTracks.map { it.sourceStartMs })
        assertEquals(listOf(1000L, 2000L), excerpt.audioTracks.map { it.durationMs })
    }

    @Test fun transitionAlreadyInProgressKeepsBothSidesOfCut() {
        val b = clip.copy(id = "b")
        val transition = ClipTransition(clip.id, b.id, JunctionTransitionPlan.DISSOLVE, 1000)
        val p = VideoProject(clips = listOf(clip, b), transitions = listOf(transition))
        val sample = ExportSamplePlan.prepare(p, 10_200, 3000) { 0 }
        val join = JunctionTransitionPlan.project(sample.project).single()
        assertEquals(200L, sample.playFromMs - join.cutMs)
        assertEquals(500L, join.halfMs)
        assertEquals(3000L, sample.playUntilMs - sample.playFromMs)
    }
}
