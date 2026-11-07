package br.com.uaicortes

import br.com.uaicortes.subtitles.core.SubtitleMotionPolicy
import org.junit.Assert.*
import org.junit.Test

class SubtitleStudioMotion204Test {
    @Test fun modernPresetsUseSharedCoreAndPreserveCueFields() {
        val old = SubtitleCue(id="c", text="Você está pronto?", startMs=300, endMs=2400,
            textCase="upper", speakerId="voz-2", x=.3f, y=.82f,
            wordTimings=listOf(SubtitleWordTiming(300L,700L)))
        assertEquals(8, SubtitleStudioPresets.recommended.size)
        assertTrue(SubtitleStudioPresets.recommended.count { it.animation in SubtitleMotionPolicy.styles } >= 6)
        for (preset in SubtitleStudioPresets.recommended) {
            val modified = SubtitleStyles.applyToCue(old, preset)
            assertEquals(old.id, modified.id)
            assertEquals(old.text, modified.text)
            assertEquals(old.startMs, modified.startMs)
            assertEquals(old.endMs, modified.endMs)
            assertEquals(old.wordTimings, modified.wordTimings)
            assertEquals(old.speakerId, modified.speakerId)
            assertEquals(old.x, modified.x, .0001f)
            assertEquals(old.y, modified.y, .0001f)
            val restored = SubtitleCue.read(modified.json())
            assertEquals(modified.animation, restored.animation)
            assertEquals(modified.text, restored.text)
        }
    }

    @Test fun bothRenderersUseSameMotionFrameWithoutChangingOriginalText() {
        val original=SubtitleCue(id="cue1",text="Olá, Minas!",startMs=1000,endMs=3000,
            animation=SubtitleAnimations.CINEMATIC,textCase="upper")
        for (at in listOf(1000L, 1100L, 1600L, 2700L, 2990L)) {
            val frame = SubtitleAnimations.frame(original, at)
            val expected = SubtitleMotionPolicy.evaluate(original.animation,
                at-original.startMs, original.endMs-original.startMs)
            assertEquals(expected.alpha, frame.alpha, .00001f)
            assertEquals(expected.scale, frame.scale, .00001f)
            assertEquals(expected.slideX, frame.slideX, .00001f)
            assertEquals(expected.slideY, frame.slideY, .00001f)
            assertEquals("OLÁ, MINAS!", frame.text)
        }
        assertEquals("Olá, Minas!", original.text)
    }
}
