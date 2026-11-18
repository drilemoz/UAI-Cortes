package br.com.uaicortes

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException

class SubtitleCloud307Test {
    @Test fun knownRejectionCannotLookLikeAnUnknownTranscription() {
        val issue = SubtitleCloudIssue.from(SubtitleCloudPhase.TRANSCRIBE, GeminiHttpException(429, "ignore"))
        assertEquals("step=transcribe reason=http http=429", issue.diagnostic())
        assertFalse(issue.needsRecoveryReview)
        assertTrue(issue.message().contains("Cota"))
    }

    @Test fun timeoutIsUncertainOnlyOnceInferenceIsReached() {
        assertFalse(SubtitleCloudIssue.from(SubtitleCloudPhase.UPLOAD_BYTES, SocketTimeoutException()).needsRecoveryReview)
        assertTrue(SubtitleCloudIssue.from(SubtitleCloudPhase.TRANSCRIBE, SocketTimeoutException()).needsRecoveryReview)
        assertFalse(SubtitleCloudIssue(SubtitleCloudPhase.READ_RESULT, SubtitleCloudReason.INVALID_RESULT).needsRecoveryReview)
    }

    @Test fun textReviewDoesNotAlterCueTimeAndRefusesNumberChanges() {
        val original = SubtitleCue(id = "a", text = "Ola, temos 3 casas", startMs = 500, endMs = 2400,
            wordTimings = listOf(SubtitleWordTiming(500, 950), SubtitleWordTiming(950, 1250),
                SubtitleWordTiming(1250, 1500), SubtitleWordTiming(1500, 2400)))
        fun answer(text: String) = JSONObject().put("edits", JSONArray().put(
            JSONObject().put("n", 1).put("text", text)))
        val results = SubtitleProofreadPolicy.parse(listOf(original), answer("Olá, temos 3 casas."))
        assertEquals(original.copy(text = "Olá, temos 3 casas."), results.single().after)
        assertTrue(SubtitleProofreadPolicy.parse(listOf(original), answer("Olá, temos 4 casas.")).isEmpty())
        val newer = original.copy(startMs = 520)
        assertEquals(listOf(newer), SubtitleProofreadPolicy.apply(listOf(newer), results))
    }
}
