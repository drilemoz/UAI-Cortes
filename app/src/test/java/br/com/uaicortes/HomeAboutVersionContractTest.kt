package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class HomeAboutVersionContractTest {
    @Test fun aboutAndWhatsNewShareLatestVersion() {
        assertEquals("0.1.99", AppReleaseNotes.VERSION_NAME)
        assertEquals(100, AppReleaseNotes.VERSION_CODE)
        assertEquals(AppReleaseNotes.VERSION_NAME, AppReleaseNotes.forAbout().first().version)
        assertEquals(AppReleaseNotes.forAbout(), AppReleaseNotes.forWhatsNew())
    }
    @Test fun homeChangeIsDocumented() {
        assertTrue(AppReleaseNotes.current.features.any { it.key == "home-preview-about" })
        assertTrue(AppReleaseNotes.current.features.any { it.key == "manager-version-sync" })
    }
}
