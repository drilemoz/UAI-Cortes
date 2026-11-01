package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleNarrationReuseTest {
    private val lines = listOf(
        NarrationSubtitleLine("late", "Terceira frase.", 3000, 4000),
        NarrationSubtitleLine("early", "Primeira   frase.\nOlá!", 100, 900),
        NarrationSubtitleLine("empty", "   ", 1200, 1900),
        NarrationSubtitleLine("invalid", "Não entra", 2000, 1999),
        NarrationSubtitleLine("middle", "Segunda frase.", 1000, 2000)
    )

    @Test fun sortsAndFiltersInvalid() {
        assertEquals(listOf("early", "middle", "late"), SubtitleNarrationReuse.eligible(lines).map { it.id })
    }

    @Test fun selectedOnlyInTimelineOrder() {
        assertEquals("Primeira frase. Olá!\nTerceira frase.",
            SubtitleNarrationReuse.assembled(lines, setOf("late", "early")))
    }

    @Test fun nothingSelectedNeverCreatesDraft() {
        val original = lines.toList()
        assertEquals("", SubtitleNarrationReuse.assembled(lines, emptySet()))
        assertEquals(original, lines)
    }

    @Test fun maxLengthDoesNotTruncate() {
        val exact = "x".repeat(SubtitleNarrationReuse.MAX_CHARACTERS)
        assertTrue(SubtitleNarrationReuse.canImport(exact))
        assertFalse(SubtitleNarrationReuse.canImport(exact + "x"))
        assertFalse(SubtitleNarrationReuse.canImport("  "))
        assertEquals(exact + "x", SubtitleNarrationReuse.assembled(
            listOf(NarrationSubtitleLine("1", exact + "x", 0, 100)), setOf("1")))
    }

    @Test fun ignoresDuplicateIdentifiersAndBlankIds() {
        val duplicate = listOf(NarrationSubtitleLine("a", "Primeira", 0, 1),
            NarrationSubtitleLine("a", "Segunda", 2, 3),
            NarrationSubtitleLine("", "Sem ID", 3, 4))
        assertEquals("Primeira", SubtitleNarrationReuse.assembled(duplicate, setOf("a", "")))
    }
}
