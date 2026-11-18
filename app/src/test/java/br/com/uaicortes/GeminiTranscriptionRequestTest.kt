package br.com.uaicortes

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class GeminiTranscriptionRequestTest {
    private fun config(body: JSONObject) = body.getJSONObject("generation_config").getJSONObject("transcription_config")

    @Test fun buildsInlineWithPortugueseWordTimestampsAndSpeakers() {
        val wav = byteArrayOf(82, 73, 70, 70, 0)
        val body = GeminiTranscriptionRequest.body(GeminiTranscriptionRequest.inline(wav), speakers = true)
        val input = body.getJSONArray("input").getJSONObject(0)
        val mode = config(body).getJSONObject("mode")
        assertEquals("models/gemini-3.5-transcribe", body.getString("model"))
        assertEquals("audio", input.getString("type"))
        assertEquals("audio/wav", input.getString("mime_type"))
        assertArrayEquals(wav, Base64.getDecoder().decode(input.getString("data")))
        assertEquals("pt-BR", config(body).getJSONArray("language_codes").getString(0))
        assertEquals("verbatim", mode.getString("type"))
        assertEquals("word", mode.getJSONArray("timestamp_granularities").getString(0))
        assertEquals("speaker", mode.getString("diarization_mode"))
    }

    @Test fun largeAudioUsesFileShapeAndExactSizeBoundary() {
        val ceiling = GeminiTranscriptionRequest.INLINE_MAX_BYTES.toLong()
        assertFalse(GeminiTranscriptionRequest.useInline(0))
        assertTrue(GeminiTranscriptionRequest.useInline(ceiling))
        assertFalse(GeminiTranscriptionRequest.useInline(ceiling + 1))
        val uri = "https://generativelanguage.googleapis.com/v1beta/files/example"
        val input = GeminiTranscriptionRequest.file(uri)
        assertEquals("file", input.getString("type"))
        assertEquals(uri, input.getString("file_uri"))
        assertFalse(input.has("data"))
    }

    @Test fun probeUsesEnglishWordTimestampsWithoutDiarization() {
        val config = config(GeminiTranscriptionRequest.body(
            GeminiTranscriptionRequest.inline(byteArrayOf(1)), speakers = false, sample = true))
        assertEquals("en-US", config.getJSONArray("language_codes").getString(0))
        val mode = config.getJSONObject("mode")
        assertFalse(mode.has("diarization_mode"))
        assertEquals("word", mode.getJSONArray("timestamp_granularities").getString(0))
    }
}
