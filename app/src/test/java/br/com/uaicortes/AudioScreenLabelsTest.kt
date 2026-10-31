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
}
