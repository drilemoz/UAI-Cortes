package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ExportSubtitlePolicyTest {
    @Test fun flagsOverlapsAndTruncatedCuesWithoutModifyingTheirTimes() {
        val cues = listOf(
            SubtitleCue(text="Primeira", startMs=0, endMs=1500),
            SubtitleCue(text="Segunda", startMs=1000, endMs=2000),
            SubtitleCue(text="Terceira", startMs=2000, endMs=2600),
            SubtitleCue(text="Fim cortado", startMs=2700, endMs=4000),
            SubtitleCue(text="Fora", startMs=3000, endMs=4000),
            SubtitleCue(text="Inválida", startMs=500, endMs=450),
            SubtitleCue(text="  ", startMs=0, endMs=1000))
        val result = ExportSubtitlePolicy.summarize(cues, 3000)
        assertEquals(4, result.valid)
        assertEquals(1, result.overlaps)
        assertEquals(1, result.outside)
        assertEquals(2, result.invalid)
        assertEquals(3, result.warnings.size)
        assertEquals(4000L, cues[3].endMs)
    }

    @Test fun cueStartingBeforeZeroButEndingAfterZeroIsVisible() {
        val summary = ExportSubtitlePolicy.summarize(listOf(
            SubtitleCue(text="Começou antes", startMs=-500, endMs=250),
            SubtitleCue(text="Começou no zero", startMs=0, endMs=500),
            SubtitleCue(text="Terminou antes", startMs=-500, endMs=0)
        ), 1000)
        assertEquals(2, summary.valid)
        assertEquals(1, summary.invalid)
        assertEquals(1, summary.overlaps)
    }

    @Test fun adjacentCuesDoNotOverlapButSimultaneousOnesDo() {
        assertEquals(0, ExportSubtitlePolicy.summarize(listOf(
            SubtitleCue(text="A", startMs=0, endMs=1000),
            SubtitleCue(text="B", startMs=1000, endMs=2000)), 3000).overlaps)
        assertEquals(3, ExportSubtitlePolicy.summarize(listOf(
            SubtitleCue(text="A", startMs=0, endMs=2000),
            SubtitleCue(text="B", startMs=0, endMs=2000),
            SubtitleCue(text="C", startMs=0, endMs=2000)), 3000).overlaps)
    }
}
