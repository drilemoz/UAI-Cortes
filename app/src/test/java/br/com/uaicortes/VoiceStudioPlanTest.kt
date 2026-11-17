package br.com.uaicortes

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VoiceStudioPlanTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun suggestsConversationalHumorAndFormatsConservativeEstimate() {
        val text = "Um causo do Fiat 147. " + List(191) { "palavra" }.joinToString(" ")
        val tone = VoiceStudioPlan.suggest(text)
        assertEquals("Humorística", tone.preset)
        assertEquals(100L, VoiceStudioPlan.plannedSeconds(text, VoiceStudioPlan.AUTO, tone))
        assertEquals("1:40", VoiceStudioPlan.time(100))
        assertEquals("1:00", VoiceStudioPlan.time(60))
        assertEquals("55s", VoiceStudioPlan.time(55))
        assertEquals("1,3s", VoiceStudioPlan.measured(1279))
        assertTrue(VoiceStudioPlan.estimateMs("texto.\n\n1234!", VoiceStudioPlan.AUTO, tone) >
            VoiceStudioPlan.estimateMs("texto palavra", VoiceStudioPlan.AUTO, tone))
        assertTrue(VoiceStudioPlan.tones.all { toneOption ->
            GeminiNarrationStyling.presets.any { it.label == toneOption.preset }
        })
    }

    @Test fun scenesKeepEveryWordAndUseSelectedWindows() {
        val text = List(51) { "palavra${it + 1}" }.joinToString(" ")
        val tone = VoiceStudioPlan.suggest(text)
        for (seconds in listOf(7L, 10L)) {
            val mode = if (seconds == 7L) VoiceStudioMode.SEVEN else VoiceStudioMode.TEN
            val scenes = VoiceStudioPlan.scenes(text, mode, emptyList(), VoiceStudioPlan.AUTO, tone)
            assertEquals(text, scenes.joinToString(" ") { it.text })
            assertEquals(scenes.indices.map { it * seconds * 1000 }, scenes.map { it.startMs })
        }
        val windows = listOf(2000L, 4000L, 1000L)
        val project = VoiceStudioPlan.scenes(text, VoiceStudioMode.PROJECT, windows, VoiceStudioPlan.AUTO, tone)
        assertEquals(windows, project.map { it.windowMs })
        assertEquals(listOf(0L, 2000L, 6000L), project.map { it.startMs })
        assertEquals(text, project.joinToString(" ") { it.text })
    }

    @Test fun resumesOnlyMissingWavesAndPadsBeforeNextScene() {
        val root = temp.newFolder()
        val scenes = listOf(VoiceStudioPlan.Scene(0, 7000, "Olá"),
            VoiceStudioPlan.Scene(7000, 7000, "Mundo"))
        VoiceStudioBatch(root, "same").save(0, wave(1000))
        val restored = VoiceStudioBatch(root, "same")
        assertEquals(listOf(1), restored.completed(scenes).mapIndexedNotNull { i, x -> i.takeIf { x == null } })
        assertTrue(VoiceStudioBatch(root, "other").completed(scenes).all { it == null })
        restored.save(1, wave(1500, 16000))
        val combined = restored.assemble(scenes, restored.completed(scenes))
        assertEquals(8500L, combined.durationMs)
        fun sampleAt(ms: Int) = combined.bytes[44 + ms * 24 * 2]
        assertEquals(7.toByte(), sampleAt(900))
        assertEquals(0.toByte(), sampleAt(1000))
        assertEquals(0.toByte(), sampleAt(6999))
        assertEquals(7.toByte(), sampleAt(7000))
    }

    @Test fun overflowCannotShiftOrCutSpeech() {
        val batch = VoiceStudioBatch(temp.newFolder(), "overflow")
        batch.save(0, wave(7200))
        val scenes = listOf(VoiceStudioPlan.Scene(0, 7000, "Olá"))
        assertThrows(IllegalArgumentException::class.java) { batch.assemble(scenes, batch.completed(scenes)) }
        assertEquals(7200L, batch.existing(0)?.second)
    }

    @Test fun genericRateLimitDoesNotConfirmDailyQuota() {
        assertTrue(isDailyTtsQuotaId("GenerateRequestsPerDayPerProjectPerModel-FreeTier"))
        assertFalse(isDailyTtsQuotaId("GenerateRequestsPerMinutePerProjectPerModel-FreeTier"))
        assertFalse(GeminiApiError(429, "RESOURCE_EXHAUSTED").dailyQuota)
        assertTrue(GeminiApiError(429, "RESOURCE_EXHAUSTED", true).message!!.contains("diária"))
    }

    private fun wave(ms: Int, rate: Int = 24000): GeminiWave {
        val count = ms * rate / 1000
        val bytes = ByteBuffer.allocate(44 + count * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + count * 2); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(rate); putInt(rate * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(count * 2)
            repeat(count) { putShort(7) }
        }.array()
        return GeminiWave(bytes, ms.toLong())
    }
}
