package br.com.uaicortes

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class VoiceToneTest {
    @Test fun legacyAndInvalidValuesAreSafe() {
        assertFalse(VoiceTone.read(null).enabled)
        val invalid = VoiceTone.read(JSONObject()
            .put("preset", "unknown").put("intensity", 900)
            .put("bassDb", 30).put("clarityDb", -900))
        assertEquals(VoiceTone.OFF, invalid.preset)
        assertEquals(1f, invalid.intensity, 0f)
        assertEquals(4f, invalid.bassDb, 0f)
        assertEquals(-4f, invalid.clarityDb, 0f)
    }

    @Test fun settingsRoundTripKeepsOnlySelectedProfile() {
        val before = VoiceTone(VoiceTone.WARM, .65f, 1.2f, -.8f, .5f)
        val restored = VoiceTone.read(before.json())
        assertEquals(before.preset, restored.preset)
        assertEquals(before.intensity, restored.intensity, .0001f)
        assertEquals(before.bassDb, restored.bassDb, .0001f)
    }

    @Test fun dspLimitsPeakAndResetsState() {
        val dsp = VoiceToneDsp(VoiceTone(VoiceTone.WARM, .85f), 48_000, 2)
        var maximum = 0.0
        repeat(48_000) { i ->
            val sample = .95 * sin(i * 2 * PI * 170 / 48_000)
            val output = dsp.process(doubleArrayOf(sample, sample))
            assertTrue(output.all { it.isFinite() })
            maximum = maxOf(maximum, abs(output[0]))
        }
        assertTrue(maximum <= .971)
        dsp.reset()
        assertTrue(dsp.process(doubleArrayOf(0.0, 0.0)).all { abs(it) < 1e-9 })
    }

    @Test fun stereoFilterStatesStaySeparated() {
        val dsp = VoiceToneDsp(VoiceTone(VoiceTone.NATURAL), 44_100, 2)
        repeat(1000) { i ->
            val v = if (i == 0) 0.8 else 0.0
            val output = dsp.process(doubleArrayOf(v, 0.0))
            assertTrue(abs(output[1]) < 1e-10)
        }
    }
}
