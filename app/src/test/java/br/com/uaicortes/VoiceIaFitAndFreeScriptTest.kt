package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class VoiceIaFitAndFreeScriptTest {
    private fun wav(silenceMs: Int, toneMs: Int): ByteArray {
        val rate = 16000
        val total = (2 * silenceMs + toneMs) * rate / 1000
        val data = ByteArray(total * 2)
        val from = silenceMs * rate / 1000
        val to = from + toneMs * rate / 1000
        for (i in from until to) {
            val v = if ((i / 20) % 2 == 0) 8000 else -8000
            data[2 * i] = v.toByte(); data[2 * i + 1] = (v shr 8).toByte()
        }
        val out = ByteArray(44 + data.size)
        fun w16(p: Int, n: Int) { out[p] = n.toByte(); out[p + 1] = (n shr 8).toByte() }
        fun w32(p: Int, n: Int) { w16(p, n); w16(p + 2, n shr 16) }
        "RIFF".toByteArray().copyInto(out, 0); w32(4, out.size - 8)
        "WAVEfmt ".toByteArray().copyInto(out, 8); w32(16, 16); w16(20, 1); w16(22, 1)
        w32(24, rate); w32(28, rate * 2); w16(32, 2); w16(34, 16)
        "data".toByteArray().copyInto(out, 36); w32(40, data.size)
        data.copyInto(out, 44)
        return out
    }

    @Test fun trimsEdgeSilenceKeepingBreathingRoom() {
        val trimmed = VoiceIaWavStretch.trimSilence(wav(1000, 1000))
        assertNotNull(trimmed)
        assertTrue(trimmed!!.durationMs in 1070L..1090L)
    }

    @Test fun fitReturnsTrimmedWhenItAlreadyFitsTheSubtitle() {
        val fitted = VoiceIaWavStretch.fit(wav(1000, 1000), 1500L)
        assertTrue(fitted.durationMs <= 1500L)
    }

    @Test fun freeScriptKeepsOrderStableIdsAndNeverPassesProjectEnd() {
        val plan = VoiceIaFreeScript.plan("a\n\nb", 0L, 10_000L)
        assertEquals(listOf("livre-1", "livre-2"), plan.cues.map { it.id })
        assertEquals(0L, plan.cues[0].startMs)
        assertEquals(1050L, plan.cues[0].endMs)
        assertEquals(1200L, plan.cues[1].startMs)
        assertTrue(plan.cues.all { it.endMs <= 10_000L && it.endMs > it.startMs })
    }

    @Test fun freeScriptWithoutRoomIsEmpty() {
        assertTrue(VoiceIaFreeScript.plan("a", 9_900L, 10_000L).cues.isEmpty())
    }
}
