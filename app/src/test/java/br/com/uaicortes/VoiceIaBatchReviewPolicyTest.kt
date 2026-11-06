package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class VoiceIaBatchReviewPolicyTest {
    @Test fun countsMissingPendingAndOverlappingReviewSignals() {
        val total = VoiceIaBatchReviewPolicy.review(listOf(
            VoiceIaReviewItem("1", "cliente", true, false, false, false, false),
            VoiceIaReviewItem("2", "cliente", true, true, false, true, false),
            VoiceIaReviewItem("3", "agiota", false, false, false, false, false),
            VoiceIaReviewItem("4", "agiota", true, false, true, false, true),
            VoiceIaReviewItem("4", "agiota", true, false, true, false, true)))
        assertEquals(4, total.total)
        assertEquals(1, total.ready)
        assertEquals(2, total.pending)
        assertEquals(1, total.missingVoice)
        assertEquals(1, total.outdated)
        assertEquals(1, total.syncReview)
        assertEquals(1, total.uncertain)
    }

    @Test fun scopedReviewDoesNotIncludeOtherCharacters() {
        val items = listOf(
            VoiceIaReviewItem("1", "cliente", true, false, false, false, false),
            VoiceIaReviewItem("2", "agiota", true, true, false, false, false))
        val filtered = VoiceIaBatchReviewPolicy.review(items.filter { it.speakerId == "cliente" })
        assertEquals(1, filtered.total)
        assertEquals(1, filtered.pending)
        assertEquals(0, filtered.ready)
    }
}
