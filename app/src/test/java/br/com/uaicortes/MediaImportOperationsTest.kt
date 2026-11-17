package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class MediaImportOperationsTest {
    @Test fun importsValidClipsAndReportsInvalidInputs() {
        val batch = MediaImportRules.clips(listOf(
            ClipSource("content://a", "A", 2000L), ClipSource("", "B", 2000L),
            ClipSource("content://bad", "X", 0L), ClipSource("content://b", "B", 5000L)))
        assertEquals(2, batch.clips.size)
        assertEquals(2, batch.rejected)
        assertEquals(listOf("content://a", "content://b"), batch.clips.map { it.uri })
    }
    @Test fun audioHonorsSourceLengthAndProjectBounds() {
        val track = MediaImportRules.audio("sound", "Som", 7000, 10000, 8000)!!
        assertEquals(8000L, track.startMs)
        assertEquals(2000L, track.durationMs)
        assertEquals(0L, track.sourceStartMs)
        assertNull(MediaImportRules.audio("sound", "Som", 100, 10000, 0))
        assertNull(MediaImportRules.audio("sound", "Som", 5000, 100, 0))
    }
}
