package br.com.uaicortes

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceAudioSearchTest {
    @Test fun matchesAccentlessMultiwordQueryInAnyOrder() {
        val key = DeviceAudioSearch.key("Narração Capítulo 04.MP3")
        assertTrue(DeviceAudioSearch.matches(key, DeviceAudioSearch.terms(" 04  NARRACAO ")))
        assertTrue(DeviceAudioSearch.matches(key, DeviceAudioSearch.terms(" ")))
        assertFalse(DeviceAudioSearch.matches(key, DeviceAudioSearch.terms("narração 05")))
    }

    @Test fun waveFilterAcceptsExtensionOrKnownMime() {
        assertTrue(DeviceAudioSearch.isWave("Diálogo.WAV", "application/octet-stream"))
        assertTrue(DeviceAudioSearch.isWave("Gravação", "audio/X-WAV"))
        assertFalse(DeviceAudioSearch.isWave("Efeito.wav.mp3", "audio/mpeg"))
    }
}
