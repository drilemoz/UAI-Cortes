package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class TextWatermarkTest {
    @Test fun emptyAndZeroDurationDoNotAddOverlay() {
        assertNull(TextWatermark().toSubtitle(1000))
        assertNull(TextWatermark(text="Canal").toSubtitle(0))
    }
    @Test fun watermarkIsIndependentAndCoversWholeVideo() {
        val cue = TextWatermark("Meu Canal", 12, 0xFF00FFFF.toInt(), .5f, .5f, .9f)
            .toSubtitle(180_000L)!!
        assertEquals("watermark-text", cue.id)
        assertEquals(0L, cue.startMs)
        assertEquals(180_000L, cue.endMs)
        assertEquals("Meu Canal", cue.text)
        assertEquals(0x8000FFFF.toInt(), cue.color)
        assertEquals(SubtitleStyles.NO_BACKGROUND, cue.backgroundColor)
    }
    @Test fun inputBoundsAreSafe() {
        val v = TextWatermark("a".repeat(100), 200, -1, Float.NaN, -10f, 10f).normalized()
        assertEquals(80, v.text.length)
        assertEquals(36, v.sizeSp)
        assertEquals(.75f, v.opacity, .001f)
        assertEquals(.1f, v.x, .001f)
        assertEquals(.95f, v.y, .001f)
    }
    @Test fun jsonRoundTripAndOptionalField() {
        val original = TextWatermark("Viagem", 22, 0xFFFFFF00.toInt(), .3f, .85f, .90f)
        val reconstructed = TextWatermark.read(JSONObject(original.json().toString()))
        assertEquals(original, reconstructed)
        val old = JSONObject().put("title", "Legado")
        assertNull(old.optJSONObject("textWatermark"))
    }
}
