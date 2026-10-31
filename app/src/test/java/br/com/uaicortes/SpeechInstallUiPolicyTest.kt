package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeechInstallUiPolicyTest {
    @Test fun noStaleExtractionAfterSuccessfulInstall() {
        assertNull(SpeechInstallUiPolicy.idleStage(true, false,
            "Descompactando e verificando o modelo…", ""))
    }

    @Test fun noDuplicateStageDuringActiveTask() {
        assertNull(SpeechInstallUiPolicy.idleStage(false, true,
            "Baixando modelo Vosk…", ""))
    }

    @Test fun failedOperationShowsErrorRatherThanOldStage() {
        assertNull(SpeechInstallUiPolicy.idleStage(false, false,
            "Descompactando e verificando o modelo…", "Falha na instalação"))
    }

    @Test fun cancelledOperationCanReportCancellation() {
        assertEquals("Importação cancelada.", SpeechInstallUiPolicy.idleStage(
            false, false, "Importação cancelada.", ""))
    }
}
