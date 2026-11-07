package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

/** Testes do catálogo de ajuda. Não simulam autenticação nem publicação. */
class SocialIntegrationGuidesTest {
    @Test fun everyDestinationHasSafeOfficialLinkAndInstructions() {
        SocialDestination.entries.forEach { dest ->
            val guide = SocialIntegrationGuides.forDestination(dest)
            assertTrue("URL precisa ser HTTPS: $dest", guide.setupUrl.startsWith("https://"))
            assertTrue("Ajuda ausente: $dest", guide.userSteps.isNotEmpty())
            assertTrue("Descrição ausente: $dest", guide.intro.isNotBlank())
            assertTrue(SocialIntegrationGuides.subtitle(dest).isNotBlank())
        }
    }

    @Test fun manualDestinationsNeverClaimOAuthOrDirectPublishing() {
        listOf(SocialDestination.KWAI, SocialDestination.FACEBOOK_PROFILE).forEach { dest ->
            val guide = SocialIntegrationGuides.forDestination(dest)
            assertTrue(guide.manualOnly)
            assertTrue(guide.developerSteps.isEmpty())
        }
    }

    @Test fun developerInstructionsDoNotAskToStoreSecretsInApk() {
        val meta = SocialIntegrationGuides.forDestination(SocialDestination.FACEBOOK_PAGE)
        assertFalse(meta.manualOnly)
        assertTrue(meta.developerSteps.any { it.contains("META_APP_ID") })
        assertTrue(meta.developerSteps.any { it.contains("Nunca coloque App Secret") })
        val tiktok = SocialIntegrationGuides.forDestination(SocialDestination.TIKTOK)
        assertTrue(tiktok.developerSteps.any { it.contains("backend seguro") })
        val youtube = SocialIntegrationGuides.forDestination(SocialDestination.YOUTUBE)
        assertTrue(youtube.developerSteps.any { it.contains("SHA-1") })
    }
}
