package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleStylesTest {
    @Test fun presetsContainDistinctAppearanceAndOriginalDefault() {
        assertEquals(22, SubtitleStyles.presets.size)
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
        val updated=SubtitleStyles.applyToCue(old, SubtitleStyles.presets.first { it.name == "Etiqueta" })
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

    @Test fun everyPresetPreservesContentTimingPositionAndSizeRegardlessOfOrdering() {
        val source = SubtitleCue(id="preserved", text="Teste com legenda", startMs=125,
            endMs=2450, x=.2f, y=.85f, sizeSp=31)
        val presets = SubtitleStyles.presets
        assertEquals("Nomes de estilos devem ser únicos", presets.size, presets.map { it.name }.toSet().size)
        for (preset in presets.reversed()) {
            val updated = SubtitleStyles.applyToCue(source, preset)
            assertEquals(preset.name, source.id, updated.id)
            assertEquals(preset.name, source.text, updated.text)
            assertEquals(preset.name, source.startMs, updated.startMs)
            assertEquals(preset.name, source.endMs, updated.endMs)
            assertEquals(preset.name, source.x, updated.x, 0f)
            assertEquals(preset.name, source.y, updated.y, 0f)
            assertEquals(preset.name, source.sizeSp, updated.sizeSp)
            assertEquals(preset.name, preset.foreground, updated.color)
            assertEquals(preset.name, preset.background, updated.backgroundColor)
        }
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

    @Test fun copyingWordColorsRequiresExplicitConsentAndMatchesWordsNotIndexes() {
        val source=SubtitleCue(id="s",text="Cachaça sim!",startMs=0,endMs=2100,
            wordColors=listOf(SubtitleWordColor(0,0xFF00CCFF.toInt())))
        val target=SubtitleCue(id="t",text="Hoje sim, cachaça",startMs=3100,endMs=5200,
            wordColors=listOf(SubtitleWordColor(0,0xFFFF0000.toInt())))
        val original=SubtitleStyles.applySelectedToAll(listOf(target),source,
            SubtitleStyles.CopyOptions(appearance=true,animation=false,wordColors=false)).single()
        assertEquals(target.wordColors,original.wordColors)
        val copied=SubtitleStyles.applySelectedToAll(listOf(target),source,
            SubtitleStyles.CopyOptions(appearance=true,animation=false,wordColors=true)).single()
        assertEquals(target.wordColors.first(),copied.wordColors.first())
        assertEquals(0xFF00CCFF.toInt(),copied.wordColors.first { it.index==2 }.color)
        assertEquals(target.text,copied.text)
        assertEquals(target.startMs,copied.startMs)
        assertEquals(target.id,copied.id)
    }

    @Test fun duplicatedWordsTransferColorsByOccurrenceIncludingUncoloredOnes() {
        val source=SubtitleCue(text="sim sim",startMs=0,endMs=3000,
            wordColors=listOf(SubtitleWordColor(1,0xFF88AAFF.toInt())))
        val target=SubtitleCue(text="SIM! sim",startMs=5000,endMs=7000)
        val colors=SubtitleStyles.matchingWordColors(source,target)
        assertEquals(1,colors.size)
        assertEquals(1,colors.single().index)
    }

    @Test fun batchMessagesDescribeRealGroups() {
        val options=SubtitleStyles.CopyOptions(appearance=false,animation=true,position=false)
        assertEquals("Aplicado: animação em 4 legendas",options.summary(4))
    }
}
