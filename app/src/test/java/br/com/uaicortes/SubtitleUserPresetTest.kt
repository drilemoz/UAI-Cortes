package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleUserPresetTest {
    private val original = SubtitleCue(id = "s1", text = "Minha fala original", startMs = 500, endMs = 1800,
        x = .3f, y = .8f, color = -1, wordColors = listOf(SubtitleWordColor(0, 0xFFFFFF00.toInt())))

    @Test fun customPresetDoesNotCopyTextTimingPositionOrWordOverrides() {
        val styled = SubtitleCue(id = "source", text = "Conteúdo particular", startMs = 0, endMs = 300,
            x = .9f, y = .15f, color = 0xFF00FFFF.toInt(), sizeSp = 34,
            backgroundColor = 0, bold = false, italic = true,
            outlineWidth = 3, shadowOffsetDp = 4,
            fontFamily = SubtitleStyles.FONT_MONO,
            animation = SubtitleAnimations.POP, animationSpeed = 1.5f,
            highlightColor = 0xFF0000FF.toInt())
        val preset = SubtitleUserPresets.add(emptyList(), "Meu modelo", styled).single()
        assertEquals("", preset.sample.text)
        assertEquals("", preset.sample.id)
        assertEquals(.5f, preset.sample.x, 0f)
        assertEquals(.92f, preset.sample.y, 0f)
        assertTrue(preset.sample.wordColors.isEmpty())
        val result = preset.applyTo(original)
        assertEquals(original.id, result.id)
        assertEquals(original.text, result.text)
        assertEquals(original.startMs, result.startMs)
        assertEquals(original.endMs, result.endMs)
        assertEquals(original.x, result.x, 0f)
        assertEquals(original.y, result.y, 0f)
        assertEquals(original.wordColors, result.wordColors)
        assertEquals(34, result.sizeSp)
        assertEquals(3, result.outlineWidth)
        assertEquals(4, result.shadowOffsetDp)
        assertEquals(SubtitleStyles.FONT_MONO, result.fontFamily)
        assertEquals(SubtitleAnimations.POP, result.animation)
        assertEquals(1.5f, result.animationSpeed, 0f)
        assertEquals(styled.highlightColor, result.highlightColor)
    }

    @Test fun localJsonRoundTripDoesNotRetainSourceWords() {
        val initial = SubtitleUserPresets.add(emptyList(), " Ciano   novo ", original.copy(
            text = "TEXTO_SUPER_PRIVADO_87654", animation = SubtitleAnimations.FADE))
        val payload = SubtitleUserPresetStore.encode(initial)
        assertFalse(payload.contains("TEXTO_SUPER_PRIVADO_87654"))
        assertFalse(payload.contains("Minha fala original"))
        assertFalse(payload.contains("startMs"))
        assertFalse(payload.contains("wordColors"))
        assertEquals("Ciano novo", SubtitleUserPresetStore.decode(payload).single().name)
        assertEquals(initial.single().sample.color,
            SubtitleUserPresetStore.decode(payload).single().sample.color)
        assertEquals(SubtitleAnimations.FADE,
            SubtitleUserPresetStore.decode(payload).single().sample.animation)
    }

    @Test fun renameAndDeletePreserveIdAndExistingStyle() {
        val styles = SubtitleUserPresets.add(emptyList(), "Minha fonte", original)
        val id = styles.single().id
        val changed = SubtitleUserPresets.rename(styles, id, "  Outra  fonte  ")
        assertEquals(id, changed.single().id)
        assertEquals("Outra fonte", changed.single().name)
        assertEquals(styles.single().sample, changed.single().sample)
        assertTrue(SubtitleUserPresets.remove(changed, id).isEmpty())
    }

    @Test fun rejectsDuplicatesAndCorruptedLibraryWithoutOverwriting() {
        val styles = SubtitleUserPresets.add(emptyList(), "Modelo", original)
        assertNotNull(SubtitleUserPresets.nameError("mOdElO", styles))
        assertThrows(IllegalArgumentException::class.java) {
            SubtitleUserPresets.add(styles, "modelo", original)
        }
        assertThrows(Exception::class.java) {
            SubtitleUserPresetStore.decode("{\"schema\":1,\"presets\":[{\"id\":\"a\",\"name\":\"X\",\"style\":{}}]}")
        }
    }
}
