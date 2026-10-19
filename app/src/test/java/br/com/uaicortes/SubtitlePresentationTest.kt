package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitlePresentationTest {
    @Test fun presentationRoundTripPreservesTimingAndTypography() {
        val cue=SubtitleCue(id="sub-29",text="Linha 1\nLinha 2",startMs=350L,endMs=3800L,
            x=.18f,y=.74f,fontFamily=SubtitleStyles.FONT_MONO,outlineWidth=2,
            shadowOffsetDp=5,shadowColor=-1,lineSpacingPercent=150,
            textAlignment=SubtitleStyles.ALIGN_RIGHT)
        val stored=SubtitleCue.read(cue.json())
        assertEquals("sub-29",stored.id)
        assertEquals(cue.text,stored.text)
        assertEquals(350L,stored.startMs)
        assertEquals(3800L,stored.endMs)
        assertEquals(.18f,stored.x,0f)
        assertEquals(.74f,stored.y,0f)
        assertEquals(SubtitleStyles.FONT_MONO,stored.fontFamily)
        assertEquals(2,stored.outlineWidth)
        assertEquals(5,stored.shadowOffsetDp)
        assertEquals(-1,stored.shadowColor)
        assertEquals(150,stored.lineSpacingPercent)
        assertEquals(SubtitleStyles.ALIGN_RIGHT,stored.textAlignment)
    }

    @Test fun legacyProjectsKeepOriginalPresentation() {
        val json=SubtitleCue(text="Antiga",startMs=10,endMs=700).json().apply {
            remove("shadowOffsetDp");remove("shadowColor")
            remove("lineSpacingPercent");remove("textAlignment")
        }
        val read=SubtitleCue.read(json)
        assertEquals(0,read.shadowOffsetDp)
        assertEquals(0xFF000000.toInt(),read.shadowColor)
        assertEquals(100,read.lineSpacingPercent)
        assertEquals(SubtitleStyles.ALIGN_CENTER,read.textAlignment)
    }

    @Test fun untrustedValuesAreRestrictedToAvailableControls() {
        val json=SubtitleCue(text="Validação",startMs=0,endMs=200).json().apply {
            put("shadowOffsetDp", 500)
            put("shadowColor", 0xFF12AB34.toInt())
            put("lineSpacingPercent", 900)
            put("textAlignment", "external-style")
        }
        val read=SubtitleCue.read(json)
        assertEquals(6,read.shadowOffsetDp)
        assertEquals(0xFF000000.toInt(),read.shadowColor)
        assertEquals(160,read.lineSpacingPercent)
        assertEquals(SubtitleStyles.ALIGN_CENTER,read.textAlignment)
        json.put("shadowOffsetDp", -100)
        json.put("lineSpacingPercent", -42)
        json.put("textAlignment", SubtitleStyles.ALIGN_LEFT)
        assertEquals(0,SubtitleCue.read(json).shadowOffsetDp)
        assertEquals(100,SubtitleCue.read(json).lineSpacingPercent)
        assertEquals(SubtitleStyles.ALIGN_LEFT,SubtitleCue.read(json).textAlignment)
    }

    @Test fun applyToAllOnlyCopiesAppearance() {
        val master=SubtitleCue(id="a",text="A",startMs=0L,endMs=1400L,
            shadowOffsetDp=4,shadowColor=-1,lineSpacingPercent=160,
            textAlignment=SubtitleStyles.ALIGN_LEFT)
        val second=SubtitleCue(id="b",text="Outro\ntexto",startMs=1510L,endMs=3600L,
            x=.13f,y=.82f,shadowOffsetDp=0)
        val actual=SubtitleStyles.applyToAll(listOf(master,second),master)[1]
        assertEquals("b",actual.id)
        assertEquals(second.text,actual.text)
        assertEquals(second.startMs,actual.startMs)
        assertEquals(second.endMs,actual.endMs)
        assertEquals(second.x,actual.x,0f)
        assertEquals(second.y,actual.y,0f)
        assertEquals(4,actual.shadowOffsetDp)
        assertEquals(-1,actual.shadowColor)
        assertEquals(160,actual.lineSpacingPercent)
        assertEquals(SubtitleStyles.ALIGN_LEFT,actual.textAlignment)
    }

    @Test fun presetsDoNotErasePresentationOptions() {
        val cue=SubtitleCue(text="Olá",startMs=0,endMs=500,
            shadowOffsetDp=2,lineSpacingPercent=130,textAlignment=SubtitleStyles.ALIGN_RIGHT)
        val edited=SubtitleStyles.applyToCue(cue,SubtitleStyles.presets.last())
        assertEquals(cue.shadowOffsetDp,edited.shadowOffsetDp)
        assertEquals(cue.lineSpacingPercent,edited.lineSpacingPercent)
        assertEquals(cue.textAlignment,edited.textAlignment)
    }
}
