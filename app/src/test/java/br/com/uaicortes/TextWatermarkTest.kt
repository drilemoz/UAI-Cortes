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
    @Test fun chosenClipDefinesHalfOpenWindowInProjectTime() {
        val first = Clip(id = "a", uri = "content://a", title = "A", sourceDurationMs = 10_000L)
        val second = Clip(id = "b", uri = "content://b", title = "B", sourceDurationMs = 8_000L,
            startMs = 1_000L, endMs = 7_000L)
        val mark = TextWatermark(text = "Canal", clipId = "b")
        val cue = requireNotNull(mark.toSubtitle(listOf(first, second)))
        assertEquals(10_000L, cue.startMs)
        assertEquals(16_000L, cue.endMs)
        assertNull(SubtitlePreviewPolicy.firstAt(listOf(cue), 9_999L))
        assertEquals(cue, SubtitlePreviewPolicy.firstAt(listOf(cue), 10_000L))
        assertNull(SubtitlePreviewPolicy.firstAt(listOf(cue), 16_000L))

        val moved = requireNotNull(mark.toSubtitle(listOf(second, first)))
        assertEquals(0L, moved.startMs)
        assertEquals(6_000L, moved.endMs)
        assertNull(mark.toSubtitle(listOf(first)))
        assertNull(mark.toSubtitle(10_000L))
    }

    @Test fun olderMarksRemainGlobalAndScopedStylePersists() {
        val old = TextWatermark.read(JSONObject().put("text", "Antes"))
        assertNull(old.clipId)
        assertEquals(0L, requireNotNull(old.toSubtitle(12_000L)).startMs)
        assertEquals(12_000L, requireNotNull(old.toSubtitle(12_000L)).endMs)
        val scoped = old.copy(clipId = "a", fontFamily = SubtitleStyles.FONT_CONDENSED)
        assertEquals(scoped, TextWatermark.read(JSONObject(scoped.json().toString())))
        assertEquals("sans-serif-condensed", SubtitleStyles.androidFamily(scoped.fontFamily))
        assertEquals("sans-serif-black", SubtitleStyles.androidFamily(SubtitleStyles.FONT_HEAVY))
        assertEquals(6, SubtitleStyles.watermarkFonts.size)
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
