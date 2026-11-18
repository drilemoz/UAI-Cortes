package br.com.uaicortes

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin
import org.junit.Assert.*
import org.junit.Test

class VoiceWindowFit305Test {
    private val clip = Clip(uri="file:///video.mp4", title="Vídeo", sourceDurationMs=6000)
    private val cue = SubtitleCue(id="a", text="Uma fala", startMs=1000, endMs=2500)

    @Test fun targetUsesNextSubtitleWhenItStartsBeforeTheEnd() {
        val next = SubtitleCue(id="b", text="Outra fala", startMs=2150, endMs=3200)
        val p = VideoProject(title="Teste", clips=listOf(clip), subtitles=listOf(next,cue))
        assertEquals(1150L, VoiceWindowFit.targetMs(p, cue))
    }

    @Test fun smallOverrunCanFitWithoutEditingTheSource() {
        val original = wave(1120)
        val untouched = original.bytes.copyOf()
        val fitted = VoiceWindowFit.adjust(original, 1000)
        assertEquals(1000L, fitted?.durationMs ?: -1L)
        assertArrayEquals(untouched, original.bytes)
        assertNull(VoiceWindowFit.adjust(original, 350))
    }

    @Test fun sameTakeIsAlreadyInsertedButNewFileStillNeedsInsertion() {
        val p = VideoProject(title="Teste", clips=listOf(clip), subtitles=listOf(cue))
        val t = VoiceIaTake(cue.id, VoiceIaPolicy.fingerprint(cue), "Kore", "model",
            "neutro", File("/som/voz.wav").path, 1000L, "")
        assertFalse(VoiceIaPolicy.alreadyInserted(p, cue, t))
        val inserted = VoiceIaPolicy.merge(p, cue, t)
        assertTrue(VoiceIaPolicy.alreadyInserted(inserted, cue, t))
        assertFalse(VoiceIaPolicy.alreadyInserted(inserted, cue, t.copy(filePath="/som/nova.wav")))
    }

    private fun wave(ms: Int): GeminiWave {
        val rate = 8000
        val frames = ms * 8
        val pcmBytes = frames * 2
        val buffer = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + pcmBytes).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2)
            .putShort(2).putShort(16).put("data".toByteArray()).putInt(pcmBytes)
        repeat(frames) { buffer.putShort((8000 * sin(it * .30)).toInt().toShort()) }
        return GeminiWave(buffer.array(), ms.toLong())
    }
}
