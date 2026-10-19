package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class EditFeaturesTest {
    @Test fun srtImportParsesTimesAndMultilineText() {
        val srt = "\uFEFF1\n00:00:01,200 --> 00:00:03,450\nPrimeira linha\nsegunda linha\n\n" +
            "2\n00:00:04.000 --> 00:00:05.250\nOutra legenda\n"
        val cues = EditFeatures.parseSrt(srt)
        assertEquals(2, cues.size)
        assertEquals(1200L,cues[0].startMs)
        assertEquals(3450L,cues[0].endMs)
        assertEquals("Primeira linha\nsegunda linha",cues[0].text)
        assertEquals(4000L,cues[1].startMs)
    }
    @Test fun srtRejectsInvalidAndLimitsCueCount() {
        val srt = "1\n00:00:10,000 --> 00:00:01,000\nInválida\n\n" +
            "2\n00:00:00,200 --> 00:00:01,000\nVálida\n\n" +
            "3\n00:00:01,000 --> 00:00:02,000\nTerceira\n"
        assertEquals(1,EditFeatures.parseSrt(srt,maxCues=1).size)
        assertEquals("Válida",EditFeatures.parseSrt(srt,maxCues=1)[0].text)
    }
    @Test fun fadeKeepsDurationAndUsesNormalizedFractions() {
        assertEquals(0f,EditFeatures.fadeFraction(0,6000,500,500),.0001f)
        assertEquals(1f,EditFeatures.fadeFraction(1500,6000,500,500),.0001f)
        assertEquals(0f,EditFeatures.fadeFraction(6000,6000,500,500),.0001f)
    }
    @Test fun invalidTransitionIsLimitedByShorterClip() {
        val t=ClipTransition("a","b","Fade preto",1000)
        assertEquals(250L,EditFeatures.transitionMs(t,500,5000))
        assertEquals(0L,EditFeatures.transitionMs(t.copy(kind="Corte direto"),500,5000))
    }
    @Test fun transitionsPersistForAdjacentPairsOnly() {
        val a=Clip(uri="a",title="a",sourceDurationMs=1000)
        val b=Clip(uri="b",title="b",sourceDurationMs=1000)
        val c=Clip(uri="c",title="c",sourceDurationMs=1000)
        val project=VideoProject(clips=listOf(a,b,c),transitions=listOf(
            ClipTransition(a.id,b.id,"Fade preto"), ClipTransition(b.id,c.id,"Fade branco")))
        val updated=EditFeatures.repositionAfterDelete(project,listOf(a,c))
        assertTrue(updated.transitions.isEmpty())
    }
    @Test fun newProjectSchemaRoundTrip() {
        val a=Clip(uri="a",title="a",sourceDurationMs=1000)
        val project=VideoProject(clips=listOf(a), subtitles=listOf(
            SubtitleCue(text="Teste",startMs=0,endMs=700)),
            audioTracks=listOf(AudioTrack("content://test/audio","Áudio",durationMs=500)))
        val restored=VideoProject.read(project.json())
        assertEquals(4,project.json().getInt("schemaVersion"))
        assertEquals(1,restored.subtitles.size)
        assertEquals(1,restored.audioTracks.size)
    }
}
