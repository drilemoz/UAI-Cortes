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
        assertEquals(6f, invalid.bassDb, 0f)
        assertEquals(-6f, invalid.clarityDb, 0f)
    }

    @Test fun settingsRoundTripKeepsOnlySelectedProfile() {
        val before = VoiceTone(VoiceTone.WARM, .65f, 1.2f, -.8f, .5f)
        val restored = VoiceTone.read(before.json())
        assertEquals(before.preset, restored.preset)
        assertEquals(before.intensity, restored.intensity, .0001f)
        assertEquals(before.bassDb, restored.bassDb, .0001f)
    }

    @Test fun expandedSettingsRoundTripAndSanitize() {
        val tone = VoiceTone(VoiceTone.STUDIO, .82f, 2f, -1f, 2f,
            trebleDb=2.5f, compression=.75f, noiseReduction=.3f,
            deEsser=.65f, autoGain=false)
        assertEquals(tone, VoiceTone.read(tone.json()))
        val unsafe = VoiceTone(VoiceTone.WARM, 1f, trebleDb=90f,
            compression=Float.NaN, noiseReduction=-8f, deEsser=9f)
        assertEquals(6f, unsafe.safe().trebleDb, 0f)
        assertEquals(.55f, unsafe.safe().compression, 0f)
        assertEquals(0f, unsafe.safe().noiseReduction, 0f)
        assertEquals(1f, unsafe.safe().deEsser, 0f)
    }

    @Test fun warmPresetIsAudiblyStrongerWithoutHardClipping() {
        fun rms(tone: VoiceTone): Double {
            val dsp = VoiceToneDsp(tone, 48_000, 1)
            var sum = 0.0
            repeat(48_000) { i ->
                val sample = 0.18 * sin(2 * PI * 170 * i / 48_000)
                val processed = dsp.process(doubleArrayOf(sample))[0]
                if (i >= 12_000) sum += processed * processed
                assertTrue(processed.isFinite() && abs(processed) < 1.0)
            }
            return kotlin.math.sqrt(sum / 36_000)
        }
        val natural = rms(VoiceTone(VoiceTone.NATURAL))
        val warmer = rms(VoiceTone(VoiceTone.WARM))
        assertTrue("Encorpada deve ter corpo perceptivelmente maior", warmer > natural * 1.06)
        assertTrue("Compensação deve recuperar volume", warmer > 0.18 / kotlin.math.sqrt(2.0))
    }

    @Test fun optionalNoiseExpanderReducesQuietPassages() {
        fun lowLevel(tone: VoiceTone): Double {
            val dsp = VoiceToneDsp(tone, 48_000, 1)
            var sum = 0.0
            repeat(48_000) { i ->
                val input = 0.003 * sin(2 * PI * 200 * i / 48_000)
                val v = dsp.process(doubleArrayOf(input))[0]
                if(i >= 24_000) sum += v*v
            }
            return kotlin.math.sqrt(sum / 24_000)
        }
        val noGate = lowLevel(VoiceTone(VoiceTone.NATURAL, noiseReduction=0f))
        val gate = lowLevel(VoiceTone(VoiceTone.NATURAL, noiseReduction=1f))
        assertTrue("Expander deve atenuar trechos baixos", gate < noGate * 0.55)
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
        assertTrue(maximum < 1.0)
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
