package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

class SubtitleWordEffectsTest {
    private val cue = SubtitleCue(id="timed",text="Uai isso é real",
        startMs=1000,endMs=3000,
        animation=SubtitleAnimations.KARAOKE,
        wordTimings=listOf(
            SubtitleWordTiming(1000,1200),SubtitleWordTiming(1200,1500),
            SubtitleWordTiming(1600,1800),SubtitleWordTiming(1900,2400)),
        wordColors=listOf(SubtitleWordColor(1,0xFF00CCFF.toInt()),
            SubtitleWordColor(3,0xFFFF2828.toInt())),
        highlightColor=0xFFFFFF00.toInt(),italic=true)

    @Test fun exactWordsFromRecognitionNotEvenDivision() {
        assertEquals(0,SubtitleWordEffects.activeWordIndex(cue,1000L))
        assertEquals(1,SubtitleWordEffects.activeWordIndex(cue,1300L))
        assertEquals(1,SubtitleWordEffects.activeWordIndex(cue,1570L))
        assertEquals(2,SubtitleWordEffects.activeWordIndex(cue,1700L))
        assertEquals(3,SubtitleWordEffects.activeWordIndex(cue,2050L))
        assertEquals(4,SubtitleWordEffects.wordRanges(cue.text).size)
        assertEquals(SubtitleWordEffects.wordRanges(cue.text)[3],
            SubtitleAnimations.frame(cue,1950L).highlighted)
    }

    @Test fun manualWordsKeepIndependentColorsAndAnimatedHighlightPriority() {
        val frames=SubtitleWordEffects.colors(cue,cue.text,
            SubtitleAnimations.frame(cue,1330L).highlighted)
        assertEquals(0xFFFFFF00.toInt(),frames.first { it.range==4..7 }.color)
        assertEquals(0xFFFF2828.toInt(),frames.first { it.range==11..14 }.color)
        assertEquals(2,frames.size)
        val fallback=cue.copy(wordTimings=emptyList())
        assertNull(SubtitleWordEffects.activeWordIndex(fallback,1400))
        assertNotNull(SubtitleAnimations.frame(fallback,1400).highlighted)
    }

    @Test fun jsonRoundTripAndCorruptedTimesAreSafelyIgnored() {
        val restored=SubtitleCue.read(cue.json())
        assertEquals(cue.wordTimings,restored.wordTimings)
        assertEquals(cue.wordColors,restored.wordColors)
        assertEquals(cue.highlightColor,restored.highlightColor)
        assertTrue(restored.italic)
        val corrupted=cue.json().put("wordTimings",JSONArray()
            .put(JSONObject().put("startMs",-999).put("endMs",1)))
        assertTrue(SubtitleCue.read(corrupted).wordTimings.isEmpty())
        val legacy=cue.json().apply {
            remove("wordTimings");remove("wordColors");remove("highlightColor");remove("italic")
        }
        val old=SubtitleCue.read(legacy)
        assertTrue(old.wordTimings.isEmpty())
        assertTrue(old.wordColors.isEmpty())
        assertFalse(old.italic)
        assertEquals(SubtitleWordEffects.DEFAULT_HIGHLIGHT,old.highlightColor)
    }

    @Test fun visualPresetDoesNotOverwriteWordTimingOrIds() {
        val styled=SubtitleStyles.applyToCue(cue,SubtitleStyles.presets[5])
        assertEquals(cue.id,styled.id)
        assertEquals(cue.startMs,styled.startMs)
        assertEquals(cue.wordTimings,styled.wordTimings)
        assertEquals(cue.wordColors,styled.wordColors)
        assertTrue(styled.outlineWidth>0)
    }
}
