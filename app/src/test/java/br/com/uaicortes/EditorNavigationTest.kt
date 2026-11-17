package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class EditorNavigationTest {
    @Test fun backPolicyKeepsCurrentNavigationSemantics() {
        assertEquals("editor", EditorNavigation.back("pip", "home", "home"))
        assertEquals("subtitles", EditorNavigation.back("autosubtitle", "home", "home"))
        assertEquals("settings", EditorNavigation.back("diagnostics", "home", "home"))
    }

    @Test fun geminiSettingsReturnToTheirOrigin() {
        assertEquals("settings", EditorNavigation.back("gemini_settings", "settings", "home"))
        assertEquals("audio", EditorNavigation.back("gemini_settings", "audio", "home"))
        assertEquals("gemini_settings", EditorNavigation.knownRoute("gemini_settings"))
    }

    @Test fun projectScreensCannotBeRestoredWithoutTheProject() {
        for (route in listOf("editor", "pip", "audio", "trim", "transition", "subtitles", "watermark", "export", "autosubtitle")) {
            assertEquals("home", EditorNavigation.restoreRoute(route, false, "home", "add"))
            assertEquals(route, EditorNavigation.restoreRoute(route, true, "home", "add"))
        }
    }

    @Test fun restoreKeepsRealGalleryAndRejectsRemovedRoutes() {
        assertEquals("media", EditorNavigation.restoreRoute("media", false, "home", "new"))
        assertEquals("home", EditorNavigation.restoreRoute("media", false, "animate", "animate_images"))
        assertEquals("home", EditorNavigation.restoreRoute("media", true, "editor", "animate_images"))
        assertEquals("media", EditorNavigation.restoreRoute("media", true, "editor", "image_insert"))
        assertEquals("editor", EditorNavigation.back("media", "editor", "editor"))
        for (route in listOf("animate", "flow_workspace", "flow_prompt")) {
            assertEquals("home", EditorNavigation.restoreRoute(route, true, "editor", "add"))
        }
        assertEquals("home", EditorNavigation.restoreRoute("media", false, "pip", "add"))
        assertEquals("home", EditorNavigation.restoreRoute("fake", true, "home", "new"))
    }
}
