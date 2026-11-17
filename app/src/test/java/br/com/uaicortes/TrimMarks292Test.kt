package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class TrimMarks292Test {
    private val clip = Clip(id = "one", uri = "content://one", title = "Cena",
        sourceDurationMs = 12_000L, startMs = 2_000L, endMs = 8_100L)

    @Test fun markFromCursorKeepsOppositeBoundAndRejectsInvertedOrTinyRanges() {
        val window = TrimMarksPolicy.clipWindow(clip)
        val from = TrimMarksPolicy.mark(window, window, 3_000L, start = true)!!
        val both = TrimMarksPolicy.mark(window, from, 7_000L, start = false)!!
        assertEquals(TrimWindow(3_000L, 7_000L), both)
        assertEquals(4_000L, both.durationMs)
        assertNull(TrimMarksPolicy.mark(window, both, 6_900L, start = true))
        assertNull(TrimMarksPolicy.mark(window, both, 2_500L, start = false))
        assertEquals(TrimWindow(3_000L, 8_100L), TrimMarksPolicy.mark(window, from, 8_500L, start = false))
    }

    @Test fun frameStepAndVisualCursorUseSourceOffsets() {
        val window = TrimMarksPolicy.clipWindow(clip)
        assertEquals(2_000L, TrimVisualMath.timeInClip(0f, 220f, 2_000L, 8_100L))
        assertEquals(5_050L, TrimVisualMath.timeInClip(110f, 220f, 2_000L, 8_100L))
        assertEquals(8_100L, TrimVisualMath.timeInClip(220f, 220f, 2_000L, 8_100L))
        assertEquals(2_000L, TrimMarksPolicy.step(window, 2_010L, -1, 25f))
        assertEquals(8_100L, TrimMarksPolicy.step(window, 8_090L, 1, 25f))
        assertEquals(3_040L, TrimMarksPolicy.step(window, 3_000L, 1, 25f))
        assertEquals(3_033L, TrimMarksPolicy.step(window, 3_000L, 1, null))
    }

    @Test fun applyAllPreservesSourcesAndMarksForDifferentClips() {
        val second = clip.copy(id = "two", uri = "content://two")
        val project = VideoProject(clips = listOf(clip, second))
        val kept = mapOf("one" to TrimWindow(3_000L, 7_000L), "two" to TrimWindow(2_000L, 6_000L))
        val edited = TrimMarksPolicy.applyAll(project, kept)!!
        assertEquals(8_000L, edited.durationMs)
        assertEquals(12_200L, project.durationMs)
        assertEquals(project.clips.map { it.uri }, edited.clips.map { it.uri })
        assertEquals(project.clips.map { it.sourceDurationMs }, edited.clips.map { it.sourceDurationMs })
        assertNull(TrimMarksPolicy.applyAll(project, kept + ("one" to TrimWindow(0L, 7_000L))))
    }

    @Test fun removalRequiresAViableLeftAndRightRemainder() {
        val keep = TrimWindow(2_000L, 7_000L)
        assertFalse(TrimMarksPolicy.canRemove(keep, keep, false))
        assertTrue(TrimMarksPolicy.canRemove(keep, keep, true))
        assertTrue(TrimMarksPolicy.canRemove(keep, TrimWindow(3_000L, 6_000L), false))
        assertFalse(TrimMarksPolicy.canRemove(keep, TrimWindow(2_100L, 6_000L), false))
        assertFalse(TrimMarksPolicy.canRemove(keep, TrimWindow(1_900L, 6_000L), false))
        assertEquals(2_000L to 2_100L, TrimVisualMath.selection(2_000L, 2_100L, 2_050L, 1, 2_000L, 2_100L))
    }
}
