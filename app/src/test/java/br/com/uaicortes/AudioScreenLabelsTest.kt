package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioScreenLabelsTest {
    @Test fun tracksIncludeVideoExtraAndLegacyMusic() {
        assertEquals(3,AudioScreenLabels.activeTrackCount(1,1,true))
        assertEquals(0,AudioScreenLabels.activeTrackCount(0,0,false))
        assertEquals(4,AudioScreenLabels.activeTrackCount(2,2,false))
    }
    @Test fun labelDoesNotExposeFullFilePath() {
        assertEquals("Adriano ElevenLabs", AudioScreenLabels.readableFileName("/Music/Adriano_ElevenLabs.m4a"))
        assertEquals("Faixa de áudio",AudioScreenLabels.readableFileName(""))
        assertEquals("Narracao",AudioScreenLabels.readableFileName("Narracao.mp3"))
    }
    @Test fun videoClipWithGeneratedIdHasReadableName() {
        assertEquals("Vídeo 1 · Áudio original",
            AudioScreenLabels.readableVideoClip("461698779_1793733324232480.mp4", 0))
        assertEquals("Vídeo 2 · Ovos Natal.mp4",
            AudioScreenLabels.readableVideoClip("Ovos Natal.mp4", 1))
        assertEquals("Vídeo 1 · Áudio original", AudioScreenLabels.readableVideoClip("", 0))
    }
}
