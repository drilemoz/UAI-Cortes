package br.com.uaicortes

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GeminiLiteTranscriptionTest {
    private fun envelope(rows: String, finish: String = "STOP") = JSONObject(
        """{"candidates":[{"finishReason":"$finish","content":{"parts":[{"thought":true,"text":"ignore"}]}}]}"""
    ).apply { getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        .put(JSONObject().put("text", """{"segments":$rows}""")) }

    @Test fun encodesInlineAndUploadedAudioForGenerateContent() {
        val inline = GeminiTranscriptionRequest.inline(byteArrayOf(1, 2, 3))
        val body = GeminiLiteTranscription.body(inline, 1500)
        val parts = body.getJSONArray("contents").getJSONObject(0).getJSONArray("parts")
        assertTrue(parts.getJSONObject(0).getString("text").contains("1500"))
        assertEquals("AQID", parts.getJSONObject(1).getJSONObject("inlineData").getString("data"))
        val config = body.getJSONObject("generationConfig")
        assertEquals("MINIMAL", config.getJSONObject("thinkingConfig").getString("thinkingLevel"))
        assertEquals("ARRAY", config.getJSONObject("responseSchema").getJSONObject("properties")
            .getJSONObject("segments").getString("type"))
        val uri = "https://generativelanguage.googleapis.com/v1beta/files/example"
        val uploaded = GeminiLiteTranscription.body(GeminiTranscriptionRequest.file(uri), 1500)
        assertEquals(uri, uploaded.getJSONArray("contents").getJSONObject(0).getJSONArray("parts")
            .getJSONObject(1).getJSONObject("fileData").getString("fileUri"))
    }

    @Test fun parsesOneLineWithoutWordTimesOrSpeakers() {
        val cue = GeminiLiteTranscription.parse(
            envelope("""[{"startMs":200,"endMs":1000,"text":"Uma\nfrase só","speaker":"inventado"}]"""), 1000).single()
        assertEquals("Uma frase só", cue.text)
        assertEquals(200L, cue.startMs)
        assertEquals(1000L, cue.endMs)
        assertTrue(cue.wordTimings.isEmpty())
        assertNull(cue.speakerId)
        assertEquals(SubtitleAnimations.NONE, cue.animation)
        assertTrue(GeminiLiteTranscription.parse(envelope("[]"), 1000).isEmpty())
    }

    @Test fun rejectsTruncationOverlapAndInvalidTimestamps() {
        val malformed = listOf(
            envelope("[]", "MAX_TOKENS"),
            envelope("""[{"startMs":-1,"endMs":300,"text":"a"}]"""),
            envelope("""[{"startMs":42.2,"endMs":300,"text":"a"}]"""),
            envelope("""[{"startMs":30,"endMs":1001,"text":"a"}]"""),
            envelope("""[{"startMs":0,"endMs":300,"text":"a"},{"startMs":299,"endMs":400,"text":"b"}]""")
        )
        malformed.forEach { assertTrue(runCatching { GeminiLiteTranscription.parse(it, 1000) }.isFailure) }
    }

    @Test fun quotaAndOverloadSwitchOnceOnlyDuringTranscription() = runBlocking {
        val route = GeminiRecoveryRoute.TRANSCRIBE
        val primary = GeminiTranscriptionRequest.MODEL
        for (code in listOf(404, 429, 503)) {
            val tried = mutableListOf<String>()
            val traces = mutableListOf<GeminiRecoveryTrace>()
            withContext(GeminiRecoveryChannel { traces += it }) {
                GeminiModelRecovery.execute(route, primary, pause = {}) { id, first ->
                    tried += id
                    if (first) throw SubtitleCloudException(SubtitleCloudIssue(
                        SubtitleCloudPhase.TRANSCRIBE, SubtitleCloudReason.HTTP, code))
                }
            }
            assertEquals(listOf(primary, GeminiLiteTranscription.MODEL), tried)
            assertEquals(code, traces.last().attempts.first().fault.http)
            assertFalse(traces.last().failed)
        }
        for (phase in SubtitleCloudPhase.entries.filter { it != SubtitleCloudPhase.TRANSCRIBE }) {
            assertNull(GeminiModelRecovery.alternate(route, primary, GeminiRecoveryFault(
                GeminiRecoveryOutcome.HTTP, 429, stage = phase.label)))
        }
        for (code in listOf(400, 401, 402, 403, 408, 500, 504)) {
            assertNull(GeminiModelRecovery.alternate(route, primary, GeminiRecoveryFault(
                GeminiRecoveryOutcome.HTTP, code, stage = SubtitleCloudPhase.TRANSCRIBE.label)))
        }
        assertNull(GeminiModelRecovery.alternate(route, GeminiLiteTranscription.MODEL, GeminiRecoveryFault(
            GeminiRecoveryOutcome.HTTP, 503, stage = SubtitleCloudPhase.TRANSCRIBE.label)))
    }
}
