package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class WhatsNewReleaseNotesTest {
    @Test fun aboutAndWhatsNewShowExactlyTheSameChangesInTheSameOrder() {
        assertEquals(AppReleaseNotes.forAbout(), AppReleaseNotes.forWhatsNew())
        assertEquals(AppReleaseNotes.VERSION_NAME, AppReleaseNotes.forAbout().first().version)
        assertTrue(AppReleaseNotes.forAbout().all { release ->
            release.features.isNotEmpty() && release.features.all {
                it.key.isNotBlank() && it.title.isNotBlank() && it.description.isNotBlank()
            }
        })
    }

    @Test fun newFeaturesTriggerOnceButARepeatedBuildDoesNot() {
        val current = AppReleaseNotes.currentFingerprint
        assertTrue(WhatsNewPolicy.shouldOpen(emptySet(), current))
        val afterFirstDismiss = WhatsNewPolicy.acknowledged(emptySet(), current)
        assertFalse(WhatsNewPolicy.shouldOpen(afterFirstDismiss, current))
        assertEquals(afterFirstDismiss, WhatsNewPolicy.acknowledged(afterFirstDismiss, current))
        // Alterar uma novidade deve permitir outra abertura.
        val changedFeatures = AppReleaseNotes.current.features +
            ReleaseFeature("novo-recurso", "Recurso novo", "Alteração de verdade", "sparkles")
        val changedFingerprint = AppReleaseNotes.fingerprintFor(changedFeatures)
        assertNotEquals(current, changedFingerprint)
        assertTrue(WhatsNewPolicy.shouldOpen(afterFirstDismiss, changedFingerprint))
    }

    @Test fun reorderingOrChangingACaptionChangesTheFingerprint() {
        val features = AppReleaseNotes.current.features
        assertEquals(AppReleaseNotes.currentFingerprint, AppReleaseNotes.fingerprintFor(features.toList()))
        assertNotEquals(AppReleaseNotes.currentFingerprint, AppReleaseNotes.fingerprintFor(features.reversed()))
        assertNotEquals(AppReleaseNotes.currentFingerprint,
            AppReleaseNotes.fingerprintFor(features.drop(1) + features.first().copy(title = "Novo título")))
        assertFalse(WhatsNewPolicy.shouldOpen(emptySet(), ""))
    }

    @Test fun whatsNewReturnPathIsSafeForAutomaticAndManualEntry() {
        assertEquals("whats_new", EditorNavigation.knownRoute("whats_new"))
        assertEquals("whats_new", EditorNavigation.restoreRoute("whats_new", false, "home", "new"))
        assertEquals("home", EditorNavigation.back("whats_new", "home", "home", "new"))
        assertEquals("settings", EditorNavigation.back("whats_new", "settings", "home", "new"))
        assertEquals("about", EditorNavigation.back("whats_new", "about", "home", "new"))
    }
}
