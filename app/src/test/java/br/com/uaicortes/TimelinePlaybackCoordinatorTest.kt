package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class TimelinePlaybackCoordinatorTest {
    @Test fun supportsEmptyAndBounds() {
        assertNull(TimelinePlaybackCoordinator.target(emptyList(), 100))
        assertEquals(0L, TimelinePlaybackCoordinator.projectPosition(emptyList(), 5, 100))
        assertEquals(TimelineSeekTarget(0, 0), TimelinePlaybackCoordinator.target(listOf(1000, 2000), -200))
        assertEquals(TimelineSeekTarget(1, 2000), TimelinePlaybackCoordinator.target(listOf(1000, 2000), 6000))
    }

    @Test fun crossingClipsUsesProjectCoordinates() {
        val durations = listOf(1_000L, 2_000L, 3_000L)
        assertEquals(TimelineSeekTarget(0, 1_000), TimelinePlaybackCoordinator.target(durations, 1_000))
        assertEquals(TimelineSeekTarget(1, 1), TimelinePlaybackCoordinator.target(durations, 1_001))
        assertEquals(TimelineSeekTarget(2, 1), TimelinePlaybackCoordinator.target(durations, 3_001))
        assertEquals(TimelineSeekTarget(1, 1_500), TimelinePlaybackCoordinator.step(durations, 3_500, -1_000))
        assertEquals(TimelineSeekTarget(2, 2_500), TimelinePlaybackCoordinator.step(durations, 3_500, 2_000))
    }

    @Test fun computesGlobalPlayhead() {
        val durations = listOf(1_000L, 2_000L)
        assertEquals(1_150, TimelinePlaybackCoordinator.projectPosition(durations, 1, 150))
        assertEquals(3_000, TimelinePlaybackCoordinator.projectPosition(durations, 55, 9_000))
        assertEquals(0, TimelinePlaybackCoordinator.projectPosition(durations, -3, -50))
    }

    @Test fun zeroDurationsAndOverflowDoNotCrash() {
        assertEquals(TimelineSeekTarget(0, 0), TimelinePlaybackCoordinator.target(listOf(0L, 100L), 0))
        assertEquals(TimelineSeekTarget(1, 50), TimelinePlaybackCoordinator.target(listOf(0L, 100L), 50))
        assertEquals(Long.MAX_VALUE, TimelinePlaybackCoordinator.total(listOf(Long.MAX_VALUE, 800L)))
        assertEquals(Long.MAX_VALUE, TimelinePlaybackCoordinator.projectPosition(listOf(Long.MAX_VALUE, 100L), 1, 100L))
        assertEquals(TimelineSeekTarget(0, Long.MAX_VALUE), TimelinePlaybackCoordinator.target(listOf(Long.MAX_VALUE, 100L), Long.MAX_VALUE))
    }
}
