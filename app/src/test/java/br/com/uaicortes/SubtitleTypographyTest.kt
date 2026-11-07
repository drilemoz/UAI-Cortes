package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleTypographyTest {
    @Test fun fontsAreLocalAndUnknownFallsBack() {
        // O Subtitle Studio adicionou Cursiva na 0.1.202: não descartar a fonte nem regredir o catálogo.
        assertEquals(listOf("sans", "serif", "mono", "cursive"), SubtitleStyles.fonts.map { it.first })
        assertEquals(4, SubtitleStyles.fonts.map { it.first }.distinct().size)
        assertEquals("sans", SubtitleStyles.normalizeFont("invalid/remote-font"))
        assertEquals("sans", SubtitleStyles.normalizeFont(""))
        assertEquals("serif", SubtitleStyles.normalizeFont("serif"))
        assertEquals("mono", SubtitleStyles.normalizeFont("mono"))
        assertEquals("cursive", SubtitleStyles.normalizeFont("cursive"))
        assertEquals("sans", SubtitleStyles.normalizeFont("https://example.com/font.otf"))
    }

    @Test fun cursiveTypographySurvivesProjectRoundTrip() {
        val original = SubtitleCue(id="cursive-local", text="A estrada continua", startMs=210L,
            endMs=1900L, fontFamily=SubtitleStyles.FONT_CURSIVE)
        val restored = SubtitleCue.read(original.json())
        assertEquals(SubtitleStyles.FONT_CURSIVE, restored.fontFamily)
        assertEquals(original.text, restored.text)
        assertEquals(original.startMs, restored.startMs)
        assertEquals(original.endMs, restored.endMs)
    }

    @Test fun typographyRoundTripPreservesTimingAndPosition() {
        val original = SubtitleCue(id="typed", text="Alô mundo", startMs=345L, endMs=2421L,
            x=.24f, y=.72f, fontFamily=SubtitleStyles.FONT_SERIF, outlineWidth=3,
            outlineColor=0xFFFFFFFF.toInt())
        val restored = SubtitleCue.read(original.json())
        assertEquals(original.id, restored.id)
        assertEquals(original.text, restored.text)
        assertEquals(original.startMs, restored.startMs)
        assertEquals(original.endMs, restored.endMs)
        assertEquals(original.x, restored.x, 0f)
        assertEquals(original.y, restored.y, 0f)
        assertEquals(SubtitleStyles.FONT_SERIF, restored.fontFamily)
        assertEquals(3, restored.outlineWidth)
        assertEquals(0xFFFFFFFF.toInt(), restored.outlineColor)
    }

    @Test fun legacyAndCorruptTypographyHaveSafeDefaults() {
        val legacyJson = SubtitleCue(text="Legado", startMs=0L,endMs=1000L).json()
        legacyJson.remove("fontFamily")
        legacyJson.remove("outlineWidth")
        legacyJson.remove("outlineColor")
        val legacy = SubtitleCue.read(legacyJson)
        assertEquals(SubtitleStyles.FONT_SANS, legacy.fontFamily)
        assertEquals(0, legacy.outlineWidth)
        assertEquals(0xFF000000.toInt(), legacy.outlineColor)

        legacyJson.put("fontFamily", "https://invalid")
        legacyJson.put("outlineWidth", 120)
        val recovered = SubtitleCue.read(legacyJson)
        assertEquals(SubtitleStyles.FONT_SANS, recovered.fontFamily)
        assertEquals(4, recovered.outlineWidth)
        legacyJson.put("outlineWidth", -50)
        assertEquals(0, SubtitleCue.read(legacyJson).outlineWidth)
    }

    @Test fun applyAppearanceDoesNotChangeTextTimingOrPosition() {
        val a = SubtitleCue(id="a", text="A", startMs=10L,endMs=900L,
            fontFamily=SubtitleStyles.FONT_MONO, outlineWidth=4,
            outlineColor=0xFFFFFFFF.toInt())
        val b = SubtitleCue(id="b",text="B",startMs=1001L,endMs=2099L,x=.11f,y=.4f)
        val copied = SubtitleStyles.applyToAll(listOf(a,b),a)
        assertEquals("b", copied[1].id)
        assertEquals("B", copied[1].text)
        assertEquals(1001L, copied[1].startMs)
        assertEquals(2099L, copied[1].endMs)
        assertEquals(.11f,copied[1].x,0f)
        assertEquals(.4f,copied[1].y,0f)
        assertEquals(SubtitleStyles.FONT_MONO,copied[1].fontFamily)
        assertEquals(4,copied[1].outlineWidth)
        assertEquals(0xFFFFFFFF.toInt(),copied[1].outlineColor)
    }
}
