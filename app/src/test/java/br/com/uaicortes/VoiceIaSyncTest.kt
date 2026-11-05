package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class VoiceIaSyncTest {
    @Test fun largeDifferenceRequiresRegenerationInsteadOfLocalSpeed() {
        val result = VoiceIaSync.review(500, 2800, 2900, 10000, 4500)
        assertEquals(2300, result.availableMs)
        assertEquals(2200, result.excessMs)
        assertEquals(2100, result.nextOverlapMs)
        assertFalse(result.canStretch)
        assertTrue(result.requiredSpeed > 1.9)
    }
    @Test fun smallDifferenceCanBeAdjustedWithConfirmation() {
        val result = VoiceIaSync.review(0, 3000, 3010, 10000, 3270)
        assertEquals(3000, result.availableMs)
        assertTrue(result.canStretch)
        assertTrue(result.mild)
    }
    @Test fun nextCueLimitsAvailableTime() {
        val result = VoiceIaSync.review(500, 4000, 2400, 10000, 2100)
        assertEquals(1900, result.availableMs)
        assertEquals(200, result.nextOverlapMs)
    }
    @Test fun suggestedGeminiTimingDoesNotRewriteScript() {
        val prompt = VoiceIaSync.guidance("humorística", 2300, brisk = true)
        assertTrue(prompt.contains("approximately 2.3 seconds"))
        assertTrue(prompt.contains("never omit or rewrite"))
    }
    @Test fun shortenWavPreservesInputAndPitchReferenceSampleRate() {
        val rate = 24000
        val samples = 24000
        val wav = ByteArray(44 + samples * 2)
        fun put16(index: Int, value: Int) { wav[index] = value.toByte(); wav[index + 1] = (value ushr 8).toByte() }
        fun put32(index: Int, value: Int) { put16(index, value); put16(index + 2, value ushr 16) }
        "RIFF".toByteArray().copyInto(wav, 0); put32(4, wav.size - 8)
        "WAVEfmt ".toByteArray().copyInto(wav, 8)
        put32(16, 16); put16(20, 1); put16(22, 1)
        put32(24, rate); put32(28, rate * 2); put16(32, 2); put16(34, 16)
        "data".toByteArray().copyInto(wav, 36); put32(40, samples * 2)
        for (i in 0 until samples) {
            val sample = (10000 * kotlin.math.sin(2.0 * Math.PI * 440 * i / rate)).toInt()
            put16(44 + 2 * i, sample)
        }
        val unchanged = wav.copyOf()
        val result = VoiceIaWavStretch.shorten(wav, 910)
        assertArrayEquals(unchanged, wav)
        assertTrue(result.durationMs in 905..915)
        assertEquals(910 * rate / 1000 * 2 + 44, result.bytes.size)
        assertEquals(1.0, result.durationMs / 910.0, .01)
        assertEquals("RIFF", String(result.bytes, 0, 4))
    }
}
