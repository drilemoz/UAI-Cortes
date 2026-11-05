package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class FlowBatchPolicyTest {
    private val draft = FlowPromptDraft(
        context = "Personagem com roupa azul, praça ao fundo",
        scenes = listOf("Caminha pela praça.", "Corre ao ver um cachorro.", "Para e acena."),
        mode = "create"
    )

    @Test fun selectionBuildsStableLabelsAndContext() {
        val plan = FlowBatchPolicy.prepare(draft, setOf(2, 0))
        assertEquals(listOf("flow-cena-1", "flow-cena-3"), plan.requests.map { it.label })
        assertEquals(listOf(0, 2), plan.scenes.map { it.index })
        assertTrue(plan.requests[0].prompt.contains("Personagem com roupa azul"))
        assertTrue(plan.requests[0].prompt.contains("Caminha pela praça"))
        assertFalse(plan.requests[0].prompt.contains("Corre ao ver um cachorro"))
        assertEquals(900, plan.requests[0].maxOutputTokens)
    }

    @Test fun unchangedSceneMayApplyButEditsInvalidate() {
        val plan = FlowBatchPolicy.prepare(draft, setOf(0, 1))
        val ref = plan.scenes[0]
        assertEquals(64, ref.fingerprint.length)
        assertTrue(FlowBatchPolicy.mayApply(draft, ref, "Nova descrição"))
        assertFalse(FlowBatchPolicy.mayApply(draft.copy(context = "Outro local"), ref, "Nova descrição"))
        assertFalse(FlowBatchPolicy.mayApply(draft.copy(scenes = listOf("Outra", "Corre", "Para")), ref, "Nova descrição"))
        assertFalse(FlowBatchPolicy.mayApply(draft, ref, "x".repeat(1801)))
    }

    @Test fun rejectsOneBlankOrNineScenes() {
        assertThrows(IllegalArgumentException::class.java) {
            FlowBatchPolicy.prepare(draft, setOf(0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FlowBatchPolicy.prepare(draft.copy(scenes = listOf("", "tem texto")), setOf(0, 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FlowBatchPolicy.prepare(FlowPromptDraft(scenes = List(12) { "Cena $it" }), (0..8).toSet())
        }
    }

    @Test fun resultMapsMetadataAndNeverAppliesError() {
        val plan = FlowBatchPolicy.prepare(draft, setOf(0, 2))
        val accepted = GeminiBatchStatus("batches/abcd", "JOB_STATE_SUCCEEDED",
            listOf("flow-cena-3" to "Nova 3", "flow-cena-1" to "Nova 1"), "")
        assertEquals("Nova 1", FlowBatchPolicy.resultFor(accepted, plan.scenes, plan.scenes[0]))
        assertEquals("Nova 3", FlowBatchPolicy.resultFor(accepted, plan.scenes, plan.scenes[1]))
        assertNull(FlowBatchPolicy.resultFor(accepted.copy(state = "JOB_STATE_RUNNING"), plan.scenes, plan.scenes[0]))
        assertNull(FlowBatchPolicy.resultFor(accepted.copy(issues = setOf("flow-cena-1")), plan.scenes, plan.scenes[0]))
    }

    @Test fun legacyResponsesMayUseDocumentedIndexOrderButNotArbitraryKeys() {
        val plan = FlowBatchPolicy.prepare(draft, setOf(0, 2))
        val response = GeminiBatchStatus("batches/abcd", "JOB_STATE_SUCCEEDED",
            listOf("Texto 1" to "Primeira", "Texto 2" to "Segunda"), "")
        assertEquals("Segunda", FlowBatchPolicy.resultFor(response, plan.scenes, plan.scenes[1]))
        assertNull(FlowBatchPolicy.resultFor(response.copy(results = listOf("outro" to "Errado")),
            plan.scenes, plan.scenes[0]))
    }
}
