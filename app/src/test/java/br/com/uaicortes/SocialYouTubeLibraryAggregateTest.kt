package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialYouTubeLibraryAggregateTest {
    @Test fun mergeRemovesDuplicateIdsAndSortsByDateDesc() {
        val older = YouTubeSocialApi.PreviousVideo(id = "a1234567890", title = "Primeiro", publishedAt = "2026-11-01")
        val newer = YouTubeSocialApi.PreviousVideo(id = "b1234567890", title = "Segundo", publishedAt = "2026-11-03")
        val merged = SocialYouTubeLibraryAggregate.merge(mapOf(
            "c1" to listOf(older, newer),
            "c2" to listOf(older.copy(channelTitle = "Outro"))
        ))
        assertEquals(listOf("b1234567890", "a1234567890"), merged.map { it.id })
    }

    @Test fun loadedChannelCountConsidersOnlyNonEmptyLists() {
        val count = SocialYouTubeLibraryAggregate.loadedChannelCount(mapOf(
            "c1" to emptyList(),
            "c2" to listOf(YouTubeSocialApi.PreviousVideo("a1234567890", "Ok", "2026-11-01"))
        ))
        assertEquals(1, count)
    }

    @Test fun totalViewsSumsKnownValuesOnly() {
        val total = SocialYouTubeLibraryAggregate.totalViews(listOf(
            YouTubeSocialApi.PreviousVideo("a1234567890", "Um", "2026-11-01", viewCount = 12),
            YouTubeSocialApi.PreviousVideo("b1234567890", "Dois", "2026-11-02", viewCount = null),
            YouTubeSocialApi.PreviousVideo("c1234567890", "Tres", "2026-11-03", viewCount = 8)
        ))
        assertTrue(total == 20L)
    }
}
