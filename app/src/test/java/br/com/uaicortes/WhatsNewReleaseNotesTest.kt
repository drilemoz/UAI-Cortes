package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class WhatsNewReleaseNotesTest {
    @Test fun initialWhatsNewOnlyShowsCurrentVersionWhileHistoryIsAvailableSeparately() {
        assertEquals(listOf(AppReleaseNotes.current), AppReleaseNotes.forWhatsNew())
        assertEquals(1, AppReleaseNotes.forWhatsNew().size)
        assertEquals(AppReleaseNotes.VERSION_NAME, AppReleaseNotes.forWhatsNew().single().version)
        assertEquals(1, AppReleaseNotes.forAbout().size)
        assertEquals(1, AppReleaseNotes.forAbout().single().features.size)
        assertEquals(AppReleaseNotes.recent.drop(1), AppReleaseNotes.forHistory())
        assertEquals(AppReleaseNotes.recent.drop(3), AppReleaseNotes.forOlderAbout())
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
        // A versão atual pode registrar uma única novidade; nesse caso, reversed() não muda a ordem.
        // Testar reordenação com um par independente das notas publicadas no app.
        val second = ReleaseFeature("teste-ordem-ci71", "Segunda novidade", "Descrição para teste", "check")
        val pair = listOf(features.first(), second)
        assertNotEquals(AppReleaseNotes.fingerprintFor(pair), AppReleaseNotes.fingerprintFor(pair.reversed()))
        assertNotEquals(AppReleaseNotes.currentFingerprint,
            AppReleaseNotes.fingerprintFor(features.mapIndexed { index, feature ->
                if (index == 0) feature.copy(title = "Novo título") else feature
            }))
        assertFalse(WhatsNewPolicy.shouldOpen(emptySet(), ""))
    }

    @Test fun aSingleFeatureHasNoOrderChangeButItsContentStillChangesFingerprint() {
        val original = ReleaseFeature("unico", "Título", "Descrição", "check")
        val singleton = listOf(original)
        assertEquals(AppReleaseNotes.fingerprintFor(singleton), AppReleaseNotes.fingerprintFor(singleton.reversed()))
        assertNotEquals(AppReleaseNotes.fingerprintFor(singleton),
            AppReleaseNotes.fingerprintFor(listOf(original.copy(description = "Outra descrição"))))
    }

    @Test fun whatsNewReturnPathIsSafeForAutomaticAndManualEntry() {
        assertEquals("whats_new", EditorNavigation.knownRoute("whats_new"))
        assertEquals("whats_new", EditorNavigation.restoreRoute("whats_new", false, "home", "new"))
        assertEquals("home", EditorNavigation.back("whats_new", "home", "home", "new"))
        assertEquals("settings", EditorNavigation.back("whats_new", "settings", "home", "new"))
        assertEquals("about", EditorNavigation.back("whats_new", "about", "home", "new"))
    }
}
