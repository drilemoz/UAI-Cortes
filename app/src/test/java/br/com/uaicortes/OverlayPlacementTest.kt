package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class OverlayPlacementTest {
    private fun item(id: String, start: Long = 0, size: Float = .34f) =
        PlacementItem(id, start, 10000L, .5f, .5f, size, size)

    @Test fun fourSimultaneousItemsDontOverlap() {
        val items = (1..4).map { item("$it") }
        val map = OverlayPlacement.arrange(items, "4")!!
        assertEquals(4, map.size)
        for (a in items) for (b in items) if (a.id != b.id) {
            val p = map.getValue(a.id); val q = map.getValue(b.id)
            assertFalse(OverlayPlacement.intersects(a, b, p.first, p.second, q.first, q.second))
        }
    }
    @Test fun nonOverlappingTimeCanReuseSameSpace() {
        val items = listOf(item("a"), item("b", 10000L))
        val map = OverlayPlacement.arrange(items)!!
        assertEquals(map.getValue("a"), map.getValue("b"))
    }
    @Test fun impossibleLayoutIsRejected() {
        val items = (1..4).map { item("$it", size = .9f) }
        assertNull(OverlayPlacement.arrange(items))
    }
    @Test fun userChosenPlacementHasPriority() {
        val a = item("a").copy(x = .35f, y = .35f)
        val b = item("b").copy(x = .35f, y = .35f)
        val map = OverlayPlacement.arrange(listOf(a, b), "b")!!
        assertEquals(.35f, map.getValue("b").first, .0001f)
        assertEquals(.35f, map.getValue("b").second, .0001f)
        assertNotEquals(map.getValue("a"), map.getValue("b"))
    }
    @Test fun fourLargePipsCanReflowFromCenter() {
        val layers = (1..4).map { item("$it", size = .45f) }
        val map = OverlayPlacement.arrange(layers, "4")!!
        assertEquals(4, map.size)
        for (a in layers) for (b in layers) if (a.id != b.id) {
            val p = map.getValue(a.id); val q = map.getValue(b.id)
            assertFalse(OverlayPlacement.intersects(a, b, p.first, p.second, q.first, q.second))
        }
    }
    @Test fun rejectsMoreThanFour() {
        assertNull(OverlayPlacement.arrange((1..5).map { item("$it") }))
    }
}
