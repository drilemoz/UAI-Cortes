package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GeneratedVoiceMixTest {
    private fun clip(id: String, volume: Float = 1f, image: Boolean = false) =
        Clip(id = id, uri = "content://fixture/$id", title = id,
            sourceDurationMs = 3000L, startMs = 2000L, volume = volume, isImage = image)

    @Test fun usesTimelineIntervalsAndDoesNotLowerAtAnExclusiveBoundary() {
        val clips = listOf(clip("a"), clip("b"), clip("c"))
        val mixed = GeneratedVoiceMix.lowerOriginal(clips, 1000L, 1000L)
        assertEquals(listOf(1f, .1f, 1f), mixed.map { it.volume })
        assertSame(clips[0], mixed[0])
        assertEquals(clips[1].copy(volume = .1f), mixed[1])
    }

    @Test fun preservesQuietAndMutedVolumesAndSkipsImages() {
        val clips = listOf(clip("a", .05f), clip("b", 0f), clip("c", 1f, image = true), clip("d", .6f))
        val mixed = GeneratedVoiceMix.lowerOriginal(clips, 500L, 3100L)
        assertEquals(listOf(.05f, 0f, 1f, .1f), mixed.map { it.volume })
        assertEquals(mixed, GeneratedVoiceMix.lowerOriginal(mixed, 500L, 3100L))
    }
}
