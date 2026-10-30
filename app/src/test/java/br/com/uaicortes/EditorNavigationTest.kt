package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class EditorNavigationTest {
    @Test fun backPolicyKeepsCurrentNavigationSemantics() {
        assertEquals("editor", EditorNavigation.back("pip", "home", "home", "new"))
        assertEquals("subtitles", EditorNavigation.back("autosubtitle", "home", "home", "new"))
        assertEquals("editor", EditorNavigation.back("animate", "home", "home", "append"))
        assertEquals("home", EditorNavigation.back("animate", "editor", "editor", "new"))
        assertEquals("settings", EditorNavigation.back("diagnostics", "home", "home", "new"))
        assertEquals("animate", EditorNavigation.back("media", "home", "animate", "new"))
    }

    @Test fun projectScreensCannotBeRestoredWithoutTheProject() {
        for (route in listOf("editor", "pip", "audio", "trim", "transition", "subtitles", "watermark", "export", "autosubtitle")) {
            assertEquals("home", EditorNavigation.restoreRoute(route, false, "home", "add"))
            assertEquals(route, EditorNavigation.restoreRoute(route, true, "home", "add"))
        }
    }

    @Test fun restoreKeepsNewGalleryAndImageAnimationWithoutVideoProject() {
        assertEquals("media", EditorNavigation.restoreRoute("media", false, "home", "new"))
        assertEquals("media", EditorNavigation.restoreRoute("media", false, "animate", "animate_images"))
        assertEquals("home", EditorNavigation.restoreRoute("media", false, "pip", "add"))
        assertEquals("animate", EditorNavigation.restoreRoute("animate", false, "home", "new"))
        assertEquals("home", EditorNavigation.restoreRoute("fake", true, "home", "new"))
    }
}
