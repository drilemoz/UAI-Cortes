package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class FrameSnapshotProjectTest {
    @Test fun singleFrameKeepsPipAndSubtitleTimeWithoutTouchingOriginal() {
        val source = Clip(uri = "content://local/video", title = "V", sourceDurationMs = 10000L)
        val pip = Overlay(uri = "content://local/pip", isImage = false,
            durationMs = 5000L, startAtMs = 2000L, sourceStartMs = 400L)
        val cue = SubtitleCue(text = "uma duas", startMs = 1000L, endMs = 7000L,
            wordTimings = listOf(SubtitleWordTiming(1000L, 2600L), SubtitleWordTiming(2600L, 7000L)))
        val original = VideoProject(clips = listOf(source), overlays = listOf(pip), subtitles = listOf(cue))
        val job = FrameSnapshotProject.prepare(original, 3200L, 1080)!!
        assertEquals(500L, job.targetInVideoMs)
        assertEquals(1080, job.requestedShortSide)
        assertEquals(2700L, job.project.clips.single().startMs)
        assertEquals(4300L, job.project.clips.single().endMs)
        assertEquals(0L, job.project.overlays.single().startAtMs)
        assertEquals(1600L, job.project.overlays.single().durationMs)
        assertEquals(1100L, job.project.overlays.single().sourceStartMs)
        assertEquals(-1700L, job.project.subtitles.single().startMs)
        assertEquals(4300L, job.project.subtitles.single().endMs)
        assertEquals(-100L, job.project.subtitles.single().wordTimings[1].startMs)
        assertFalse(job.directSourceFrame)
        assertEquals(3200L, job.sourceFrame.sourceTimeMs)
        assertEquals(0L, source.startMs)
        assertEquals(2000L, pip.startAtMs)
    }

    @Test fun frameAtBoundaryUsesPreviousClipSameAsSeekCoordinator() {
        val first = Clip(uri = "content://local/a", title = "A", sourceDurationMs = 4000L)
        val second = Clip(uri = "content://local/b", title = "B", sourceDurationMs = 5000L)
        val job = FrameSnapshotProject.prepare(VideoProject(clips = listOf(first, second)), 4000L, 720)!!
        assertEquals(first.uri, job.project.clips.single().uri)
        assertEquals(first.uri, job.sourceFrame.uri)
        assertTrue(job.directSourceFrame)
        assertEquals(3999L, job.sourceFrame.sourceTimeMs)
        assertEquals(4000L, job.project.clips.single().endMs)
    }

    @Test fun plainVideoBypassesMuxerButSubtitleNeedsComposition() {
        val source = Clip(uri = "content://local/video", title = "V", sourceDurationMs = 6000L)
        val plain = VideoProject(clips = listOf(source))
        val basic = FrameSnapshotProject.prepare(plain, 4200L, 720)!!
        assertTrue(basic.directSourceFrame)
        assertEquals(4200L, basic.sourceFrame.sourceTimeMs)
        val withSubtitle = plain.copy(subtitles = listOf(SubtitleCue(
            text = "fala", startMs = 3800L, endMs = 4700L)))
        assertFalse(FrameSnapshotProject.prepare(withSubtitle, 4200L, 720)!!.directSourceFrame)
        assertTrue(FrameSnapshotProject.prepare(withSubtitle, 5000L, 720)!!.directSourceFrame)
    }

    @Test fun cannotMisrepresentTransitionAsSingleFrame() {
        val a = Clip(uri = "content://local/a", title = "A", sourceDurationMs = 4000L)
        val b = Clip(uri = "content://local/b", title = "B", sourceDurationMs = 5000L)
        val project = VideoProject(clips = listOf(a, b),
            transitions = listOf(ClipTransition(a.id, b.id, "Dissolver", 700L)))
        assertNull(FrameSnapshotProject.prepare(project, 3990L, 1080))
    }
}
