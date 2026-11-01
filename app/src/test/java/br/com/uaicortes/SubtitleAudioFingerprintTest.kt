package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleAudioFingerprintTest {
    @Test fun orderAndCutsChangeFingerprint() {
        val key=SubtitleAudioFingerprint.key(listOf("project", "video-a", "0", "5000", "video-b"))
        assertEquals(key, SubtitleAudioFingerprint.key(listOf("project", "video-a", "0", "5000", "video-b")))
        assertNotEquals(key,SubtitleAudioFingerprint.key(listOf("project", "video-b", "0", "5000", "video-a")))
        assertNotEquals(key,SubtitleAudioFingerprint.key(listOf("project", "video-a", "20", "5000", "video-b")))
        assertNotEquals(SubtitleAudioFingerprint.key(listOf("ab","c")),
            SubtitleAudioFingerprint.key(listOf("a","bc")))
    }

    @Test fun wavMustMatchExactExtractedLength() {
        val durations=listOf(1000L,2500L)
        assertTrue(SubtitleAudioFingerprint.validWavLength(44L+112_000L,durations))
        assertFalse(SubtitleAudioFingerprint.validWavLength(44L+112_001L,durations))
        assertFalse(SubtitleAudioFingerprint.validWavLength(44L,emptyList()))
        assertFalse(SubtitleAudioFingerprint.validWavLength(44L,listOf(-1L)))
    }
}
