package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SingleWord295Test {
    private val caption = SubtitleCue(text = "Quando a saudade", startMs = 100L, endMs = 950L,
        animation = SubtitleAnimations.SINGLE_WORD, animationSpeed = 2f,
        wordTimings = listOf(SubtitleWordTiming(160L, 270L),
            SubtitleWordTiming(300L, 380L), SubtitleWordTiming(500L, 890L)))

    @Test fun showsOnlyCurrentWordAndRespectsSilenceAndBoundaries() {
        val frame: (Long) -> String = { SubtitleAnimations.frame(caption, it).text }
        assertEquals("", frame(100L))
        assertEquals("Quando", frame(160L))
        assertEquals("", frame(270L))
        assertEquals("a", frame(300L))
        assertEquals("saudade", frame(889L))
        assertEquals("", frame(890L))
        assertTrue(SubtitleWordEffects.hasIndividualTimes(caption))
        assertNull(SubtitleAnimations.frame(caption, 300L).highlighted)
    }

    @Test fun missingTimesFallbackIsDeterministicWithoutChangingLegacyModes() {
        val cue = caption.copy(wordTimings = emptyList())
        assertFalse(SubtitleWordEffects.hasIndividualTimes(cue))
        assertEquals("Quando", SubtitleAnimations.frame(cue, 100L).text)
        assertEquals("saudade", SubtitleAnimations.frame(cue, 949L).text)
        assertEquals("", SubtitleAnimations.frame(cue, 950L).text)
        assertEquals("Quando a saudade", SubtitleAnimations.frame(
            caption.copy(animation = SubtitleAnimations.KARAOKE_BADGE), 300L).text)
        assertEquals(SubtitleAnimations.SINGLE_WORD, SubtitleAnimations.normalize("single_word"))
    }

    @Test fun originalPresetDoesNotInheritPinkBadgeOrUppercase() {
        val preset = SubtitleStudioPresets.recommended.single { it.animation == SubtitleAnimations.SINGLE_WORD }
        assertEquals("original", preset.textCase)
        assertEquals(SubtitleStyles.NO_BACKGROUND, preset.background)
    }
}
