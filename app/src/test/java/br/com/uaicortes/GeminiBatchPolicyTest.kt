package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class GeminiBatchPolicyTest {
    @Test fun requiresTwoToEightNonEmptyLines() {
        assertEquals(listOf("Cena A", "Cena B"), GeminiBatchPolicy.prompts(" Cena A \n\n Cena B\n"))
        assertThrows(IllegalArgumentException::class.java) { GeminiBatchPolicy.prompts("único") }
        assertThrows(IllegalArgumentException::class.java) {
            GeminiBatchPolicy.prompts((1..9).joinToString("\n") { "Cena $it" })
        }
    }
    @Test fun rejectsOversizedAndUnsupportedModels() {
        assertThrows(IllegalArgumentException::class.java) {
            GeminiBatchPolicy.prompts("x".repeat(1001) + "\n" + "mais um")
        }
        assertEquals("gemini-2.5-flash", GeminiBatchPolicy.model(" gemini-2.5-flash "))
        assertThrows(IllegalArgumentException::class.java) {
            GeminiBatchPolicy.model("gemini-2.5-flash-tts")
        }
        assertThrows(IllegalArgumentException::class.java) {
            GeminiBatchPolicy.model("../../other")
        }
    }
    @Test fun remoteIdPreventsArbitraryUrlsAndPaths() {
        assertEquals("batches/123-AB_cd", GeminiBatchPolicy.jobName(" batches/123-AB_cd "))
        assertThrows(IllegalArgumentException::class.java) {
            GeminiBatchPolicy.jobName("https://attacker.example/")
        }
        assertThrows(IllegalArgumentException::class.java) { GeminiBatchPolicy.jobName("batches/../../") }
    }
    @Test fun stateNeverPromisesReadyForUnknownResult() {
        assertEquals("Envio incerto: revisar", GeminiBatchPolicy.status("REVIEW"))
        assertEquals("Concluído", GeminiBatchPolicy.status("JOB_STATE_SUCCEEDED"))
        assertEquals("Aguardando consulta", GeminiBatchPolicy.status("unknown"))
    }
}
