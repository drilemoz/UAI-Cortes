package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class NarrationReferenceAlignmentTest {
    private val first = SubtitleNarrationTimeline.Segment("one", "Olá", 1000L, 2500L)
    private val second = SubtitleNarrationTimeline.Segment("two", "Final", 16000L, 19000L)
    private val all = listOf(first, second)

    @Test fun referenceMovesOnlyAudioPlacement() {
        val marks = mapOf("two" to 15000L)
        assertEquals(15000L, NarrationReferenceAlignment.start(second, marks))
        assertEquals(16000L, second.startMs) // A legenda original é imutável.
        assertEquals(15000L, NarrationReferenceAlignment.placed(second, marks).startMs)
        assertNull(NarrationReferenceAlignment.validate(all, marks, 20000L))
    }

    @Test fun noReferenceUsesSubtitleTimes() {
        assertEquals(16000L, NarrationReferenceAlignment.start(second, emptyMap()))
        assertTrue(NarrationReferenceAlignment.cleaned(all, mapOf("two" to 16000L)).isEmpty())
    }

    @Test fun cannotMoveOutOfProjectOrReorder() {
        assertNotNull(NarrationReferenceAlignment.validate(all, mapOf("two" to 20000L), 20000L))
        assertNotNull(NarrationReferenceAlignment.validate(all, mapOf("one" to 17000L), 20000L))
        assertNotNull(NarrationReferenceAlignment.validate(all, mapOf("two" to -1L), 20000L))
    }

    @Test fun ignoresStaleReferenceCues() {
        val cleaned = NarrationReferenceAlignment.cleaned(all, mapOf("gone" to 123L, "two" to 15400L))
        assertEquals(mapOf("two" to 15400L), cleaned)
    }

    @Test fun timeDisplaysSubsecondMarks() {
        assertEquals("00:15.500", NarrationReferenceAlignment.timeLabel(15500L))
        assertEquals("01:02.005", NarrationReferenceAlignment.timeLabel(62005L))
    }

    @Test fun referenceOffsetIsClamped() {
        assertEquals(17500L, NarrationReferenceAlignment.sourceToProject(2500L, 15000L, 20000L))
        assertEquals(19999L, NarrationReferenceAlignment.sourceToProject(9000L, 15000L, 20000L))
        assertEquals(0L, NarrationReferenceAlignment.sourceToProject(0L, -10000L, 20000L))
    }

    @Test fun lateSpeechStillWarnsWhenGeneratedWavCrossesEnd() {
        val shifted = NarrationReferenceAlignment.placed(second, mapOf("two" to 18500L))
        val result = SubtitleNarrationTimeline.inspect(
            listOf(SubtitleNarrationTimeline.Recorded(shifted, 2000L)), 20000L)
        assertTrue(result.single().blocking)
    }
}
