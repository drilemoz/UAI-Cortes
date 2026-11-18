package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleFileExportTest {
    @Test fun srtOrdersAndFormatsMilliseconds() {
        val cues=listOf(
            SubtitleCue(id="b",text="Segundo",startMs=62_345,endMs=64_000),
            SubtitleCue(id="a",text="Primeiro",startMs=10,endMs=900))
        val actual=SubtitleFileExport.render(cues)
        assertTrue(actual.startsWith("1\n00:00:00,010 --> 00:00:00,900\nPrimeiro\n\n"))
        assertTrue(actual.contains("2\n00:01:02,345 --> 00:01:04,000\nSegundo"))
        assertEquals("Segundo",cues.first().text)
    }

    @Test fun invalidCuesAreSkipped() {
        val actual=SubtitleFileExport.render(listOf(SubtitleCue(text="Invalida", startMs=10,endMs=10)))
        assertEquals("",actual)
    }
    @Test fun bothFileTypesCarryReimportableTimesWithoutTheSameMime() {
        val cue = SubtitleCue(id="a", text="Primeira linha\n\nSegunda linha", startMs=7_610L, endMs=9_260L)
        val types = SubtitleFileExport.Format.entries
        assertEquals(listOf("application/x-subrip", "text/plain"), types.map { it.mimeType })
        assertEquals(listOf("UaiCortes-legendas.srt", "UaiCortes-legendas.txt"), types.map { it.fileName })
        val content = SubtitleFileExport.render(listOf(cue))
        val restored = EditFeatures.parseSrt(content).single()
        assertEquals(cue.startMs, restored.startMs)
        assertEquals(cue.endMs, restored.endMs)
        assertEquals("Primeira linha\nSegunda linha", restored.text)
        assertFalse(SubtitleFileExport.Format.SRT.hasExpectedExtension("UaiCortes-legendas.srt.txt"))
        assertTrue(SubtitleFileExport.Format.SRT.hasExpectedExtension("Cópia (1).SRT"))
        assertTrue(SubtitleFileExport.Format.TXT.hasExpectedExtension("Cópia.txt"))
    }
}
