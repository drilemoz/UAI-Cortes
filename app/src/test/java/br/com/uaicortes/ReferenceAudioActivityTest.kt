package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

internal class ReferenceAudioActivityTest {
    @Test fun `window around subtitle at end stays inside project`() {
        val window = ReferenceAudioActivity.window(18_000, 19_800, 20_000)
        assertEquals(15_000L, window.fromMs)
        assertEquals(20_000L, window.untilMs)
    }

    @Test fun `very long cue limits analysis time`() {
        val window = ReferenceAudioActivity.window(10_000, 90_000, 120_000)
        assertEquals(7_000L, window.fromMs)
        assertEquals(22_000L, window.untilMs)
    }

    @Test fun `silence has no speech suggestion`() {
        val result = ReferenceAudioActivity.detect(List(144) { 0f }, 0L, 6_000L, 3_000L)
        assertTrue(result.regions.isEmpty())
        assertNull(result.suggestedStartMs)
    }

    @Test fun `continuous speech-like energy has onset suggestion near subtitle`() {
        val levels = List(144) { i -> if (i in 53..80) 0.30f else 0.004f }
        val result = ReferenceAudioActivity.detect(levels, 7_000L, 13_000L, 10_000L)
        assertEquals(1, result.regions.size)
        assertNotNull(result.suggestedStartMs)
        assertTrue(result.suggestedStartMs!! in 9_100L..9_400L)
    }

    @Test fun `short transient does not become a region`() {
        val levels = List(144) { i -> if (i == 50) 0.7f else 0.003f }
        val result = ReferenceAudioActivity.detect(levels, 0L, 6_000L, 2_000L)
        assertTrue(result.regions.isEmpty())
        assertNull(result.suggestedStartMs)
    }

    @Test fun `uniform music-like background does not imply speech`() {
        val result = ReferenceAudioActivity.detect(List(144) { 0.08f }, 0L, 6_000L, 2_000L)
        assertTrue(result.regions.isEmpty())
    }

    @Test fun `suggestion too far from subtitle remains null`() {
        val levels = List(144) { i -> if (i in 6..25) 0.4f else 0.002f }
        val result = ReferenceAudioActivity.detect(levels, 0L, 12_000L, 8_000L)
        assertNull(result.suggestedStartMs)
    }
}
