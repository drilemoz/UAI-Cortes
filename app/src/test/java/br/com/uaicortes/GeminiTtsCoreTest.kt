package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GeminiTtsCoreTest {
    @Test fun importEnvTxtJsonAndMaskedPreview() {
        val value = "ABCDEFGHIJKLMNOP1234567890"
        assertEquals(value, GeminiKeyParser.parse("GEMINI_API_KEY=$value\n"))
        assertEquals(value, GeminiKeyParser.parse("# nota\nexport GOOGLE_API_KEY=\"$value\""))
        assertEquals(value, GeminiKeyParser.parse("{\"GEMINI_API_KEY\":\"$value\"}"))
        assertEquals(value, GeminiKeyParser.parse("{\"GOOGLE_API_KEY\":\"$value\"}"))
        assertEquals(value, GeminiKeyParser.parse("$value"))
        assertEquals("••••••••7890", GeminiKeyParser.masked(value))
    }
    @Test fun rejectsInvalidAndUnexpectedContent() {
        for (value in listOf("", "ABC", "GEMINI_API_KEY=", "OTHER=value", "abc def ghi 123", "{not json}")) {
            try { GeminiKeyParser.parse(value); fail("Should reject") } catch (_: Exception) { }
        }
    }
    @Test fun modelsAreStrictlyTts() {
        assertEquals(GeminiModels.FLASH, GeminiModels.requireSupported("gemini-3.8-flash-tts"))
        assertEquals(GeminiModels.LITE, GeminiModels.requireSupported("gemini-3.8-flash-lite-tts"))
        try { GeminiModels.requireSupported("gemini-3.8-flash"); fail() } catch (_: IllegalArgumentException) { }
    }
    private fun wave(samples: Int = 24000): ByteArray {
        val dataSize = samples * 2
        return ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataSize); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(24000); putInt(48000); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(dataSize)
            repeat(samples) { putShort(0) }
        }.array()
    }
    @Test fun validWavDurationAndTruncation() {
        val wave = wave()
        assertEquals(1000L, GeminiWaveParser.duration(wave, "audio/wav"))
        try { GeminiWaveParser.duration(wave.copyOf(wave.size - 1), "audio/wav"); fail() }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun rejectMimeAndDoubleRiffCorruption() {
        val audio = wave()
        try { GeminiWaveParser.duration(audio, "audio/mpeg"); fail() }
        catch (_: IllegalArgumentException) { }
        val wrapped = wave().copyOfRange(0, 44) + audio
        try { GeminiWaveParser.duration(wrapped, "audio/wav"); fail() }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun httpClassificationDoesNotLeakCredentials() {
        assertTrue(GeminiApiError.messageFor(401, "UNAUTHENTICATED").contains("revogada"))
        assertTrue(GeminiApiError.messageFor(404, "NOT_FOUND").contains("indisponível"))
        assertTrue(GeminiApiError.messageFor(429, "RESOURCE_EXHAUSTED").contains("Cota"))
        assertTrue(GeminiApiError.messageFor(503, "UNAVAILABLE").contains("servidor"))
    }
    @Test fun trackUriPersistedInProjectJson() {
        val project = VideoProject(audioTracks = listOf(AudioTrack(
            uri = "file:///data/user/0/br.com.uaicortes/files/gemini_narrations/narracao.wav",
            title = "Narração Gemini", startMs = 1200, durationMs = 2800, volume = 1f)))
        val restored = VideoProject.read(project.json())
        assertEquals(1200L, restored.audioTracks.single().startMs)
        assertEquals(2800L, restored.audioTracks.single().durationMs)
        assertEquals(project.audioTracks.single().uri, restored.audioTracks.single().uri)
        assertEquals(4, project.json().getInt("schemaVersion"))
    }
}
