package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class JunctionPreview307Test {
    @Test fun directCutShortensAStillAndVideoButKeepsTheSubtitlePhase() {
        val still = Clip(uri = "file:///test.png", title = "Foto", sourceDurationMs = 6000, isImage = true)
        val video = Clip(uri = "file:///test.mp4", title = "Vídeo", sourceDurationMs = 8000, startMs = 1000)
        val cue = SubtitleCue(text = "Olá", startMs = 5200, endMs = 6600,
            wordTimings = listOf(SubtitleWordTiming(5450, 6000)))
        val p = VideoProject(clips = listOf(still, video), subtitles = listOf(cue))
        val full = requireNotNull(JunctionPreviewProject.prepare(p, 0, TransitionEffects.CUT, 0))
        val short = JunctionPreviewProject.compactDirectCut(full, 0, TransitionEffects.CUT)
        assertEquals(2200L, short.project.durationMs)
        assertEquals(1100L, short.cutMs)
        assertEquals(4900L, short.project.clips.first().startMs)
        assertEquals(2100L, short.project.clips[1].endMs)
        assertEquals(300L, short.project.subtitles.single().startMs)
        assertEquals(550L, short.project.subtitles.single().wordTimings.single().startMs)
        val withMusic = full.copy(project = full.project.copy(music = Music("file:///theme.wav", "Tema")))
        assertEquals(withMusic, JunctionPreviewProject.compactDirectCut(withMusic, 0, TransitionEffects.CUT))
    }
}
