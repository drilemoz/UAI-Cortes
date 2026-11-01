package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleFileExportTest {
    @Test fun srtOrdersAndFormatsMilliseconds() {
        val cues=listOf(
            SubtitleCue(id="b",text="Segundo",startMs=62_345,endMs=64_000),
            SubtitleCue(id="a",text="Primeiro",startMs=10,endMs=900))
        val actual=SubtitleFileExport.render(cues, SubtitleFileExport.Format.SRT)
        assertTrue(actual.startsWith("1\n00:00:00,010 --> 00:00:00,900\nPrimeiro\n\n"))
        assertTrue(actual.contains("2\n00:01:02,345 --> 00:01:04,000\nSegundo"))
        assertEquals("Primeiro\nSegundo\n",SubtitleFileExport.render(cues,SubtitleFileExport.Format.TXT))
        assertEquals("Segundo",cues.first().text)
    }

    @Test fun invalidCuesAreSkipped() {
        val actual=SubtitleFileExport.render(listOf(SubtitleCue(text="Invalida", startMs=10,endMs=10)),SubtitleFileExport.Format.SRT)
        assertEquals("",actual)
    }
}
