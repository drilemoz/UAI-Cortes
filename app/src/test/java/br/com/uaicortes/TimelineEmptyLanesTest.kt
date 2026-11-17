package br.com.uaicortes

import br.com.uaicortes.TimelineKind.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineEmptyLanesTest {
    @Test fun activeLanesPrecedeEmptyRowsAndEmptyMusicIsLast() {
        assertEquals(listOf(MAIN_VIDEO, VOICE, SUBTITLE, MAIN_IMAGE, PIP, AUDIO, MUSIC),
            UnifiedTimelinePolicy.orderedLanes(setOf(MAIN_VIDEO, VOICE, SUBTITLE)))
        assertEquals(listOf(MAIN_VIDEO, MAIN_IMAGE, PIP, AUDIO, VOICE, SUBTITLE, MUSIC),
            UnifiedTimelinePolicy.orderedLanes(emptySet()))
    }

    @Test fun committedMediaPromotesLanesWithoutReorderingTheOtherFilledLanes() {
        val before = setOf(MAIN_IMAGE, VOICE, SUBTITLE)
        val added = before + setOf(MUSIC, PIP)
        assertEquals(listOf(MAIN_IMAGE, PIP, MUSIC, VOICE, SUBTITLE, MAIN_VIDEO, AUDIO),
            UnifiedTimelinePolicy.orderedLanes(added))
        assertEquals(UnifiedTimelinePolicy.orderedLanes(before),
            UnifiedTimelinePolicy.orderedLanes(added - setOf(MUSIC, PIP)))
        assertEquals(TimelineKind.entries, UnifiedTimelinePolicy.orderedLanes(TimelineKind.entries.toSet()))
    }

    @Test fun emptiesAreShorterAndShortScreensGetABoundedViewport() {
        for (kind in TimelineKind.entries) {
            assertEquals(18, UnifiedTimelinePolicy.displayHeightDp(kind, false))
            assertTrue(UnifiedTimelinePolicy.displayHeightDp(kind, true) > 18)
        }
        val screenshotRowsDp = listOf(34, 28, 24, 18, 18, 18, 18).sum()
        assertTrue(UnifiedTimelinePolicy.maxViewportHeightDp(850) >= screenshotRowsDp + 18)
        assertTrue(UnifiedTimelinePolicy.maxViewportHeightDp(570) < screenshotRowsDp + 18)
        assertEquals(232f, UnifiedTimelinePolicy.maxViewportHeightDp(1400), .001f)
    }
}
