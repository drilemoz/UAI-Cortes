package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleTimingRefinerTest {
    private fun envelope(): SubtitleVoiceEnvelope {
        val levels = IntArray(1000) { 10 }
        for (f in 100 until 200) levels[f] = 3000
        for (f in 300 until 400) levels[f] = 3000
        return SubtitleVoiceEnvelope(levels)
    }

    @Test fun correctsConstantEarlyOffsetAndKeepsText() {
        val cues = listOf(
            SubtitleCue(id = "a", text = "um", startMs = 800, endMs = 1800),
            SubtitleCue(id = "b", text = "dois", startMs = 2800, endMs = 3800))
        val result = SubtitleTimingRefiner.refine(cues, envelope(), 10_000L)
        assertEquals(200L, result.globalShiftMs)
        assertEquals(listOf("um", "dois"), result.cues.map { it.text })
        assertEquals(1000L, result.cues[0].startMs)
        assertEquals(2000L, result.cues[0].endMs)
        assertEquals(3000L, result.cues[1].startMs)
        assertEquals(4000L, result.cues[1].endMs)
    }

    @Test fun silentAudioLeavesCuesUntouched() {
        val cues = listOf(SubtitleCue(id = "a", text = "um", startMs = 800, endMs = 1800))
        val result = SubtitleTimingRefiner.refine(cues,
            SubtitleVoiceEnvelope(IntArray(1000) { 5 }), 10_000L)
        assertEquals(cues, result.cues)
        assertEquals(0L, result.globalShiftMs)
    }

    @Test fun snapsEachWordToRealPauses() {
        val levels = IntArray(1000) { 10 }
        for (range in listOf(100 until 130, 140 until 170, 180 until 210))
            for (f in range) levels[f] = 3000
        val active = SubtitleTimingRefiner.activity(levels)
        val cue = SubtitleCue(id = "a", text = "um dois tres", startMs = 1000, endMs = 2100,
            wordTimings = listOf(SubtitleWordTiming(1060, 1330), SubtitleWordTiming(1460, 1730),
                SubtitleWordTiming(1860, 2100)))
        val (refined, changed) = SubtitleTimingRefiner.refineWords(cue, active)
        assertTrue(changed > 0)
        assertEquals(listOf(1000L to 1300L, 1400L to 1700L, 1800L to 2100L),
            refined.wordTimings.map { it.startMs to it.endMs })
        assertEquals(cue.text, refined.text)
    }

    @Test fun globalShiftKeepsEverythingInsideTheVideo() {
        val cues = listOf(SubtitleCue(id = "a", text = "x", startMs = 200, endMs = 900,
            wordTimings = listOf(SubtitleWordTiming(200, 900))))
        assertEquals(-200L, SubtitleGlobalShift.allowed(cues, -500L, 1000L))
        assertEquals(100L, SubtitleGlobalShift.allowed(cues, 500L, 1000L))
        val (moved, applied) = SubtitleGlobalShift.apply(cues, 100L, 1000L)
        assertEquals(100L, applied)
        assertEquals(300L, moved[0].startMs)
        assertEquals(300L, moved[0].wordTimings[0].startMs)
        assertEquals(700L, moved[0].endMs - moved[0].startMs)
    }
}
