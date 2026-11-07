package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SocialHistory226Test {
    @Test fun filtrosSeparamOsEstadosSemInventarPublicacao() {
        assertTrue(SocialHistoryFilter.PENDING.matches(SocialPublicationStatus.PREPARED))
        assertTrue(SocialHistoryFilter.PENDING.matches(SocialPublicationStatus.SHARED_UNCONFIRMED))
        assertFalse(SocialHistoryFilter.RECEIVED.matches(SocialPublicationStatus.UPLOAD_REVIEW))
        assertTrue(SocialHistoryFilter.REVIEW.matches(SocialPublicationStatus.UPLOAD_REVIEW))
        assertTrue(SocialHistoryFilter.RECEIVED.matches(SocialPublicationStatus.CONFIRMED_BY_API))
        assertTrue(SocialHistoryFilter.RECEIVED.matches(SocialPublicationStatus.REMOTE_SUBMITTED))
    }

    @Test fun processamentoNaoEquivaleAVisibilidadePublica() {
        assertEquals("Processamento ainda não consultado", SocialYouTubeState().summary())
        val state = SocialYouTubeState(privacy = "private", processing = "succeeded",
            upload = "processed", checkedAt = 1)
        assertTrue(state.summary().contains("Processamento concluído"))
        assertEquals("Privado", state.privacyLabel())
        assertTrue(SocialYouTubeState.valid(state))
        assertFalse(SocialYouTubeState.valid(state.copy(privacy = "inventado")))
    }
}
