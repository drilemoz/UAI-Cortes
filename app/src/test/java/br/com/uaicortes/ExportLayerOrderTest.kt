package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ExportLayerOrderTest {
    @Test fun lastComposePipMustBeFirstMedia3Input() {
        val visualBottomToTop = listOf("pip-baixo", "pip-meio", "pip-cima")
        assertEquals(listOf("pip-cima", "pip-meio", "pip-baixo"),
            ExportLayerOrder.media3TopFirst(visualBottomToTop))
        assertEquals(visualBottomToTop, visualBottomToTop) // não muta a ordem persistida
        assertTrue(ExportLayerOrder.media3TopFirst(emptyList<String>()).isEmpty())
    }
}
