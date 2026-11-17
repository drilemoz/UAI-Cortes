package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleLipSyncPolicyTest {
    private val first = Clip(id = "a", uri = "video-a", title = "A", sourceDurationMs = 8000L, startMs = 2000L, endMs = 6000L)
    private val second = Clip(id = "b", uri = "video-b", title = "B", sourceDurationMs = 9000L, startMs = 1000L, endMs = 5000L)
    private fun project(vararg cues: SubtitleCue) = VideoProject(clips = listOf(first, second), subtitles = cues.toList())
    private fun cue(id: String, start: Long, end: Long) = SubtitleCue(id = id, text = "Olá mundo", startMs = start, endMs = end)
    private fun index(window: SubtitleLipSyncPolicy.Window?, time: Long): Int = window!!.timelineMs.indexOf(time).also { assertTrue(it >= 0) }

    @Test fun samplesRespectClipTrimsAndProjectTime() {
        val cue = cue("c", 4900L, 6200L)
        val p = project(cue)
        val task = SubtitleLipSyncPolicy.tasks(p, listOf(cue)).single()
        assertEquals((4500L..5300L step 100).toList(), task.start!!.timelineMs)
        assertEquals((1500L..2300L step 100).toList(), task.start.sourceMs)
        assertEquals("video-b", task.start.uri)
        assertEquals(3200L, task.end!!.sourceMs[4])
    }

    @Test fun usesIndependentVisualBoundariesAndPreservesEverythingElse() {
        val cue = cue("c", 900, 1800).copy(sizeSp = 36, speakerId = "person")
        val p = project(cue)
        val task = SubtitleLipSyncPolicy.tasks(p, listOf(cue)).single()
        val evidence = SubtitleLipSyncPolicy.Evidence(task.id, index(task.start, 1000), index(task.end, 1700), true)
        val result = SubtitleLipSyncPolicy.proposals(listOf(task), listOf(evidence), p.subtitles, p.durationMs).single()
        assertEquals(cue.copy(startMs = 1000, endMs = 1700), result.after)
    }

    @Test fun protectsAudioWordTimesAndRejectsAmbiguousOrDuplicateEvidence() {
        val words = listOf(SubtitleWordTiming(950, 1170), SubtitleWordTiming(1300, 1680))
        val cue = cue("c", 900, 1800).copy(wordTimings = words)
        val p = project(cue)
        val task = SubtitleLipSyncPolicy.tasks(p, listOf(cue)).single()
        val evidence = SubtitleLipSyncPolicy.Evidence(task.id, index(task.start, 1000), index(task.end, 1700), true)
        fun run(vararg items: SubtitleLipSyncPolicy.Evidence) =
            SubtitleLipSyncPolicy.proposals(listOf(task), items.toList(), p.subtitles, p.durationMs)
        assertEquals(cue.copy(endMs = 1700), run(evidence).single().after)
        assertTrue(run(evidence.copy(sameSpeaker = false)).isEmpty())
        assertTrue(run(evidence, evidence).isEmpty())
        assertTrue(run(evidence.copy(startIndex = 0, endIndex = 99)).isEmpty())
        assertTrue(SubtitleLipSyncPolicy.tasks(p, listOf(cue.copy(wordTimings = words.take(1)))).isEmpty())
    }

    @Test fun ignoresShotsAcrossCutsAndActiveOverlaysAndLimitsCombinedExpansion() {
        val cross = cue("cross", 3700, 4290)
        assertTrue(SubtitleLipSyncPolicy.tasks(project(cross), listOf(cross)).isEmpty())
        val cue = cue("lip", 1000, 1800)
        val other = cue("other", 2200, 3200)
        val p = project(cue, other)
        val tasks = SubtitleLipSyncPolicy.tasks(p, p.subtitles)
        val a = SubtitleLipSyncPolicy.Evidence(1, null, index(tasks[0].end, 2100), true)
        val b = SubtitleLipSyncPolicy.Evidence(2, index(tasks[1].start, 1900), null, true)
        assertTrue(SubtitleLipSyncPolicy.proposals(tasks, listOf(a, b), p.subtitles, p.durationMs).isEmpty())
        val pip = Overlay(uri = "other.mp4", isImage = false, durationMs = 2000, startAtMs = 500)
        assertTrue(SubtitleLipSyncPolicy.tasks(p.copy(overlays = listOf(pip)), listOf(cue)).isEmpty())
        val withTransition = p.copy(transitions = listOf(ClipTransition("a", "b", "Dissolver", 600)))
        val late = cue("late", 3400, 3900)
        val boundary = SubtitleLipSyncPolicy.tasks(withTransition, listOf(late)).singleOrNull()
        assertNull(boundary?.end)
    }

    @Test fun neverAcceptsModelTimestampsAsFrameIndexes() {
        val reply = """{"results":[{"id":1,"sameSpeaker":true,"onset":2,"offset":"1800"},
            {"id":2,"sameSpeaker":"true","onset":2.5,"offset":3}]}"""
        val results = GeminiSubtitleLipSync.parseReport(reply)
        assertEquals(SubtitleLipSyncPolicy.Evidence(1, 2, null, true), results[0])
        assertEquals(SubtitleLipSyncPolicy.Evidence(2, null, 3, false), results[1])
    }
}
