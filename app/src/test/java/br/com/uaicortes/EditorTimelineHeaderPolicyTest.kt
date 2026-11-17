package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class EditorTimelineHeaderPolicyTest {
    @Test fun smallerOrEnlargedTextKeepsTimeAndHistoryAccessible() {
        assertEquals(EditorTimelineHeaderPolicy.Layout.DIRECT, EditorTimelineHeaderPolicy.layout(380f, 1f))
        assertEquals(EditorTimelineHeaderPolicy.Layout.COMPACT, EditorTimelineHeaderPolicy.layout(300f, 1f))
        assertEquals(EditorTimelineHeaderPolicy.Layout.MENU, EditorTimelineHeaderPolicy.layout(260f, 1f))
        assertEquals(EditorTimelineHeaderPolicy.Layout.MENU, EditorTimelineHeaderPolicy.layout(340f, 1.6f))
        assertEquals(listOf(1, 2, 4, 8), EditorTimelineHeaderPolicy.zoomLevels)
    }

    @Test fun seekIsValidatedInsideTheProjectRatherThanChangingDuration() {
        assertEquals(10_100L, EditorTimelineHeaderPolicy.position("00101", 10_100L))
        assertEquals(0L, EditorTimelineHeaderPolicy.position("00000", 10_100L))
        assertEquals(3_723_400L, EditorTimelineHeaderPolicy.position("0102034", 4_000_000L))
        assertNull(EditorTimelineHeaderPolicy.position("000", 0L))
        assertNull(EditorTimelineHeaderPolicy.position("00102", 10_100L))
        assertNull(EditorTimelineHeaderPolicy.position("00610", 90_000L))
        assertNull(EditorTimelineHeaderPolicy.position("", 90_000L))
    }

    @Test fun mobileToolbarShowsEdgeOfNextToolWithoutShrinkingTargets() {
        for (width in listOf(320f, 360f, 412f)) {
            val item = EditorTimelineHeaderPolicy.toolbarItemWidthDp(width, 9)
            val inner = width - 12f
            val full = ((inner + 2f) / (item + 2f)).toInt()
            val visibleNext = inner - full * (item + 2f)
            assertTrue(item >= 48f)
            assertTrue(full < 9 && visibleNext >= 15f && visibleNext < item)
        }
        val tablet = EditorTimelineHeaderPolicy.toolbarItemWidthDp(600f, 9)
        assertTrue(9 * tablet + 16f + 12f <= 600.1f)
    }
}
