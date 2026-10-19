package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleStylesTest {
    @Test fun presetsContainDistinctAppearanceAndOriginalDefault() {
        assertEquals(5, SubtitleStyles.presets.size)
        val classic = SubtitleStyles.presets.first()
        assertEquals(0xFFFFFFFF.toInt(), classic.foreground)
        assertEquals(SubtitleStyles.DEFAULT_BACKGROUND, classic.background)
        assertTrue(classic.bold)
        assertEquals(0, SubtitleStyles.NO_BACKGROUND)
        assertEquals(0, SubtitleStyles.presets.first { it.name == "Limpo" }.background)
    }

    @Test fun presetOnlyChangesAppearance() {
        val old = SubtitleCue(id="sample",text="Olá",startMs=105,endMs=2500,
            x=.3f,y=.45f,sizeSp=31)
        val updated=SubtitleStyles.applyToCue(old,SubtitleStyles.presets.last())
        assertEquals("sample", updated.id)
        assertEquals(old.text,updated.text)
        assertEquals(old.startMs,updated.startMs)
        assertEquals(old.endMs,updated.endMs)
        assertEquals(old.x,updated.x,0f)
        assertEquals(old.y,updated.y,0f)
        assertEquals(31,updated.sizeSp)
        assertEquals(SubtitleStyles.LIGHT_BACKGROUND,updated.backgroundColor)
        assertEquals(0xFF000000.toInt(),updated.color)
    }

    @Test fun copyToAllPreservesIdTextsTimingAndPositions() {
        val source=SubtitleCue(id="1",text="A",startMs=0,endMs=1200,
            color=0xFF00FFFF.toInt(),sizeSp=30,backgroundColor=0,bold=false)
        val other=SubtitleCue(id="2",text="B",startMs=1250,endMs=2200,x=.2f,y=.4f)
        val result=SubtitleStyles.applyToAll(listOf(source,other),source)
        assertEquals(listOf("1","2"),result.map { it.id })
        assertEquals("B",result[1].text)
        assertEquals(1250,result[1].startMs)
        assertEquals(2200,result[1].endMs)
        assertEquals(.2f,result[1].x,0f)
        assertEquals(.4f,result[1].y,0f)
        assertEquals(30,result[1].sizeSp)
        assertEquals(0,result[1].backgroundColor)
        assertFalse(result[1].bold)
    }

    @Test fun roundTripAndLegacyDefaults() {
        val cue=SubtitleCue(text="Legenda",startMs=200,endMs=2000,
            backgroundColor=SubtitleStyles.LIGHT_BACKGROUND,bold=false)
        val restored=SubtitleCue.read(cue.json())
        assertEquals(cue.backgroundColor,restored.backgroundColor)
        assertEquals(cue.bold,restored.bold)
        val legacy=cue.json().apply { remove("backgroundColor"); remove("bold") }
        val loaded=SubtitleCue.read(legacy)
        assertEquals(SubtitleStyles.DEFAULT_BACKGROUND,loaded.backgroundColor)
        assertTrue(loaded.bold)
    }
}
