package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class TrimSaveLogicTest {
    private val one = Clip(id = "um", uri = "content://um", title = "Um", sourceDurationMs = 10000L)
    private val two = Clip(id = "dois", uri = "content://dois", title = "Dois", sourceDurationMs = 7000L)
    private val original = VideoProject(clips = listOf(one, two))

    @Test fun savesPendingTrimAndKeepsOtherClips() {
        val result = TrimSaveLogic.applyPending(original, "um", 1000, 5000)!!
        assertEquals(1000L, result.clips[0].startMs)
        assertEquals(5000L, result.clips[0].endMs)
        assertEquals(two, result.clips[1])
        assertEquals(4000L + 7000L, result.durationMs)
        assertEquals(0L, original.clips[0].startMs)
    }
    @Test fun doesNotDiscardValidUnchangedEdit() {
        assertSame(original, TrimSaveLogic.applyPending(original, "um", 0, 10000))
    }
    @Test fun rejectsInvalidRangesWithoutModifyingProject() {
        assertNull(TrimSaveLogic.applyPending(original, "um", 5000, 5100))
        assertNull(TrimSaveLogic.applyPending(original, "um", 1000, 12000))
        assertNull(TrimSaveLogic.applyPending(original, "ausente", 0, 3000))
        assertEquals(2, original.clips.size)
    }
}
