package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

/** Tests the installed build identity and retained Home/About contracts without hardcoded versions. */
class HomeAboutVersionContractTest {
    @Test fun aboutAndWhatsNewShareLatestVersion() {
        assertEquals(BuildConfig.VERSION_NAME, AppReleaseNotes.VERSION_NAME)
        assertEquals(BuildConfig.VERSION_CODE, AppReleaseNotes.VERSION_CODE)
        assertEquals(AppReleaseNotes.VERSION_NAME, AppReleaseNotes.forAbout().first().version)
        assertEquals(listOf(AppReleaseNotes.current), AppReleaseNotes.forWhatsNew())
        assertEquals(AppReleaseNotes.recent.take(3), AppReleaseNotes.forAbout())
    }

    @Test fun homeChangeIsDocumented() {
        // The Home/About features were shipped in a previous release. They must not
        // have to remain in current.features after the app receives a new release.
        val documented = AppReleaseNotes.recent.flatMap { it.features }.map { it.key }.toSet()
        assertTrue("Falta a novidade de teste da Home em Sobre", "home-preview-about" in documented)
        assertTrue("Falta a novidade de versão do GitHub Manager", "manager-version-sync" in documented)
    }
}
