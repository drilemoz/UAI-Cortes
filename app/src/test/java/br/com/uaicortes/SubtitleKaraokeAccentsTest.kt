package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleKaraokeAccentsTest {
    private val cue = SubtitleCue(id="karaoke", text="Que história engraçada!",
        startMs=1000, endMs=4000, color=0xFFFFFFFF.toInt(),
        highlightColor=0xFFFFFF00.toInt(),
        wordTimings=listOf(SubtitleWordTiming(1000,1800),
            SubtitleWordTiming(1800,2800),SubtitleWordTiming(2800,4000)))

    @Test fun animationsFollowExactWordBoundaries() {
        for (effect in listOf(SubtitleAnimations.KARAOKE_PULSE,
            SubtitleAnimations.KARAOKE_BOLD, SubtitleAnimations.KARAOKE_UNDERLINE)) {
            val c=cue.copy(animation=effect)
            assertEquals("Que", SubtitleAnimations.frame(c,1100).highlighted?.let { c.text.substring(it) })
            assertEquals("história", SubtitleAnimations.frame(c,1900).highlighted?.let { c.text.substring(it) })
            assertEquals("engraçada!",SubtitleAnimations.frame(c,3000).highlighted?.let { c.text.substring(it) })
            assertEquals(effect,SubtitleCue.read(c.json()).animation)
        }
    }

    @Test fun wordStylesAreIndependentOfOtherWordsAndTimings() {
        val c=cue.copy(animation=SubtitleAnimations.KARAOKE_BOLD)
        val f=SubtitleAnimations.frame(c,2000)
        assertTrue(f.boldActiveWord)
        assertFalse(f.underlineActiveWord)
        val colors=SubtitleWordEffects.colors(c,f.text,f.highlighted,
            f.highlightColorOverride ?: c.highlightColor)
        assertTrue(colors.any { it.range==f.highlighted && it.color==c.highlightColor })
        assertEquals(cue.wordTimings,c.wordTimings)
        val underline=SubtitleAnimations.frame(c.copy(animation=SubtitleAnimations.KARAOKE_UNDERLINE),2000)
        assertTrue(underline.underlineActiveWord)
        assertFalse(underline.boldActiveWord)
    }

    @Test fun pulseChangesColorWithoutChangingTextGeometry() {
        val c=cue.copy(animation=SubtitleAnimations.KARAOKE_PULSE)
        val a=SubtitleAnimations.frame(c,1900)
        val b=SubtitleAnimations.frame(c,2300)
        assertEquals(c.text,a.text)
        assertEquals(a.highlighted,b.highlighted)
        assertTrue(a.highlightColorOverride!=null)
        assertTrue(b.highlightColorOverride!=null)
        assertNotEquals(a.highlightColorOverride,b.highlightColorOverride)
        assertEquals(1f,a.scale,0.0001f)
        assertEquals(1f,b.scale,0.0001f)
        assertFalse(a.boldActiveWord)
    }

    @Test fun fallbackIsApproximateButDoesNotWriteTimingData() {
        val c=cue.copy(wordTimings=emptyList(),animation=SubtitleAnimations.KARAOKE_UNDERLINE)
        val f=SubtitleAnimations.frame(c,2000)
        assertNotNull(f.highlighted)
        assertTrue(c.wordTimings.isEmpty())
        assertEquals(25,SubtitleStyles.presets.size)
        assertEquals(27,SubtitleAnimations.choices.size)
    }
}
