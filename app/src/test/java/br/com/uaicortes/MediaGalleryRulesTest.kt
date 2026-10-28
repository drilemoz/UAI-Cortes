package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class MediaGalleryRulesTest {
    @Test fun searchIgnoresCaseAndMatchesFolders() {
        assertTrue(GalleryRules.matches("FILME.MP4", "DCIM/Camera", "filme"))
        assertTrue(GalleryRules.matches("arquivo.mp4", "Movies/Viagem", "viagem"))
        assertTrue(GalleryRules.matches("qualquer.mp4", "Vídeos", "   "))
        assertFalse(GalleryRules.matches("sol.mp4", "Downloads", "lua"))
    }

    @Test fun tabsRespectMediaType() {
        assertTrue(GalleryRules.accepts(GalleryTab.RECENT, true))
        assertTrue(GalleryRules.accepts(GalleryTab.RECENT, false))
        assertTrue(GalleryRules.accepts(GalleryTab.VIDEOS, false))
        assertFalse(GalleryRules.accepts(GalleryTab.VIDEOS, true))
        assertTrue(GalleryRules.accepts(GalleryTab.IMAGES, true))
        assertFalse(GalleryRules.accepts(GalleryTab.IMAGES, false))
    }
}
