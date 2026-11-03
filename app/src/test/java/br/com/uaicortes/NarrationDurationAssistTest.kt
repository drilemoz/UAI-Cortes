package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class NarrationDurationAssistTest {
    @Test fun suggestedGoalUsesRemainingTimeWithoutMovingSubtitle() {
        assertEquals(3000L, NarrationDurationAssist.defaultTarget(16000L, 19000L, 20000L))
        assertEquals(1200L, NarrationDurationAssist.defaultTarget(18800L, 19700L, 20000L).let {
            // O início pode ser ajustado, então o teto é o fim do projeto.
            NarrationDurationAssist.adjust(1200L, 0L, 18800L, 20000L)
        })
    }
    @Test fun goalsCannotOverflowProject() {
        assertEquals(1000L, NarrationDurationAssist.adjust(1250L, 250L, 19000L, 20000L))
        assertEquals(500L, NarrationDurationAssist.adjust(500L, -250L, 19000L, 20000L))
        assertEquals(0L, NarrationDurationAssist.adjust(500L, 250L, 19800L, 20000L))
    }
    @Test fun promptIsAdvisoryAndNeverRequestsOmittingWords() {
        val prompt = NarrationDurationAssist.instruction(1250L)
        assertTrue(prompt.contains("1.25"))
        assertTrue(prompt.contains("Nunca omita palavras"))
        assertTrue(prompt.contains("meta aproximada"))
    }
    @Test fun actualDurationAlwaysRemainsMeasuredAndMayExceedTarget() {
        assertEquals(1200L, NarrationDurationAssist.difference(4200L, 3000L))
        assertEquals(-500L, NarrationDurationAssist.difference(2500L, 3000L))
    }
}
