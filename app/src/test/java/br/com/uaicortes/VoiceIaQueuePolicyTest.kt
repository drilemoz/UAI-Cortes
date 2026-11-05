package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class VoiceIaQueuePolicyTest {
    private fun item(id: String = "a") = VoiceIaQueueItem(id, "a".repeat(64), "voice-7",
        "gemini-3.8-flash-lite-tts", "Neutra", "Falar naturalmente")

    @Test fun rejectsDuplicateCuesAndSensitiveOrMalformedFields() {
        assertThrows(IllegalArgumentException::class.java) {
            VoiceIaQueuePolicy.create("project", listOf(item(), item()))
        }
        assertFalse(VoiceIaQueuePolicy.valid(item().copy(cueFingerprint = "invalid")))
        assertFalse(VoiceIaQueuePolicy.valid(item().copy(requestStyle = "x".repeat(1601))))
    }

    @Test fun interruptedCallIsReviewNeverAutomaticallyPending() {
        val queue = VoiceIaQueuePolicy.create("project", listOf(item("one"), item("two")))
        val started = VoiceIaQueuePolicy.transition(queue, "one", VoiceIaQueueStage.IN_FLIGHT)
        val recovered = VoiceIaQueuePolicy.recover(started,
            mapOf("one" to "a".repeat(64), "two" to "a".repeat(64))) { false }
        assertEquals(1, recovered.review)
        assertEquals(1, recovered.pending)
        assertEquals(VoiceIaQueueStage.REVIEW, recovered.items.first().stage)
        assertThrows(IllegalArgumentException::class.java) {
            VoiceIaQueuePolicy.transition(recovered, "one", VoiceIaQueueStage.IN_FLIGHT)
        }
        val explicitlyRetried = VoiceIaQueuePolicy.retryReviewed(recovered)
        assertEquals(2, explicitlyRetried.pending)
    }

    @Test fun verifiedWaveResolvesUncertainStateButEditedSubtitleDoesNot() {
        val started = VoiceIaQueuePolicy.transition(
            VoiceIaQueuePolicy.create("project", listOf(item())), "a", VoiceIaQueueStage.IN_FLIGHT)
        val reconciled = VoiceIaQueuePolicy.recover(started, mapOf("a" to "a".repeat(64))) { true }
        assertEquals(1, reconciled.ready)
        assertFalse(reconciled.unfinished)
        val edited = VoiceIaQueuePolicy.recover(started, mapOf("a" to "b".repeat(64))) { true }
        assertEquals(1, edited.stale)
        assertTrue(edited.unfinished)
    }

    @Test fun directRetryAndCompletionStagesAreRestricted() {
        val q = VoiceIaQueuePolicy.create("project", listOf(item()))
        assertThrows(IllegalArgumentException::class.java) {
            VoiceIaQueuePolicy.transition(q, "a", VoiceIaQueueStage.READY)
        }
        val started = VoiceIaQueuePolicy.transition(q, "a", VoiceIaQueueStage.IN_FLIGHT)
        val ready = VoiceIaQueuePolicy.transition(started, "a", VoiceIaQueueStage.READY)
        assertEquals(1, ready.ready)
    }
}
