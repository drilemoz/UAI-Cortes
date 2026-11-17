package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class AutoSubtitlePanelPolicyTest {
    private fun cue(id: String, start: Long, end: Long, text: String = "Olá mundo") =
        SubtitleCue(id = id, text = text, startMs = start, endMs = end)

    @Test fun selectedRangeRespectsSourceTrimsAcrossCuts() {
        val first = Clip(id = "a", uri = "a", title = "A", sourceDurationMs = 9000,
            startMs = 1000, endMs = 6000)
        val second = Clip(id = "b", uri = "b", title = "B", sourceDurationMs = 9000,
            startMs = 2000, endMs = 7000)
        val p = VideoProject(clips = listOf(first, second))
        val cut = subtitleSpeechSlice(p, SubtitleSpeechRange(3500, 5800))
        assertEquals(2300L, cut.durationMs)
        assertEquals(listOf(4500L, 2000L), cut.clips.map { it.startMs })
        assertEquals(listOf(6000L, 2800L), cut.clips.map { it.endMs })
        assertEquals(10_000L, p.durationMs)
    }

    @Test fun subtitleAndWordOffsetsFollowTheNarrationPosition() {
        val source = cue("new", 100, 700).copy(wordTimings = listOf(
            SubtitleWordTiming(100, 300), SubtitleWordTiming(350, 700)))
        val placed = SubtitleSpeechDraft.place(listOf(source), 4000, SubtitleSpeechRange(4000, 5000)).single()
        assertEquals(4100L, placed.startMs)
        assertEquals(4700L, placed.endMs)
        assertEquals(listOf(SubtitleWordTiming(4100, 4300), SubtitleWordTiming(4350, 4700)), placed.wordTimings)
    }

    @Test fun punctuationAndCasePreserveAllWordTimesAndColors() {
        val original = cue("a", 100, 800).copy(wordTimings = listOf(
            SubtitleWordTiming(100, 360), SubtitleWordTiming(500, 780)),
            wordColors = listOf(SubtitleWordColor(1, 15)))
        val correction = SubtitleTextRetiming.edit(original, "OLÁ, mundo!")
        assertFalse(correction.needsAlignment)
        assertEquals(original.copy(text = "OLÁ, mundo!"), correction.cue)
    }

    @Test fun insertedWordKeepsBothKnownAnchorsAndFlagsTheNewTiming() {
        val original = cue("c", 100, 800).copy(wordTimings = listOf(
            SubtitleWordTiming(100, 300), SubtitleWordTiming(550, 800)),
            wordColors = listOf(SubtitleWordColor(1, 15)))
        val correction = SubtitleTextRetiming.edit(original, "Olá grande mundo")
        assertTrue(correction.needsAlignment)
        assertEquals(listOf(SubtitleWordTiming(100, 300), SubtitleWordTiming(300, 550),
            SubtitleWordTiming(550, 800)), correction.cue.wordTimings)
        assertEquals(listOf(SubtitleWordColor(2, 15)), correction.cue.wordColors)
    }

    @Test fun redoCanFillMissingSpeechAndDoesNotRemoveCuesAcrossEdges() {
        val first = cue("first", 900, 1100, "Frase já revisada")
        val middle = cue("middle", 1700, 1900)
        val last = cue("outside", 3000, 3300)
        val old = listOf(first, middle, last)
        val range = SubtitleSpeechRange(1000, 2800)
        val invalid = cue("conflict", 1000, 1300)
        val inserted = cue("new", 1200, 1500).copy(speakerId = "unrelated")
        val replacement = cue("replacement", 1650, 1950)
        assertEquals(listOf(first, inserted.copy(speakerId = null), replacement, last),
            SubtitleSpeechDraft.redo(old, listOf(invalid, inserted, replacement), range))
        assertEquals(old, SubtitleSpeechDraft.redo(old, listOf(invalid), range))
    }
}
