package br.com.uaicortes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Só pode ser executado em emulador/aparelho Android. */
@RunWith(AndroidJUnit4::class)
class GeminiVoicePreviewCacheAndroidTest {
    private fun wave(): ByteArray {
        val length = 48000
        return ByteBuffer.allocate(44 + length).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + length); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(24000); putInt(48000); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(length)
            repeat(length / 2) { putShort(0) }
        }.array()
    }
    @Test fun savedVoiceReusedAndCorruptionRejected() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cache = GeminiVoicePreviewCache(ctx)
        val id = GeminiPreviewIdentity.id("test_key_123", "lite", "voice_1", "neutral")
        val file = cache.save(id, wave())
        try {
            assertEquals(file.absolutePath, cache.find(id)?.absolutePath)
            file.writeText("invalid")
            assertNull(cache.find(id))
        } finally { file.delete() }
    }
    @Test fun oldSampleRemainsAvailableWithoutExpiration() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cache = GeminiVoicePreviewCache(ctx)
        val id = GeminiPreviewIdentity.id("test_key_456", "lite", "voice_2", "neutral")
        val file = cache.save(id, wave())
        try {
            assertTrue(file.setLastModified(System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000))
            assertNotNull(cache.find(id))
        } finally { file.delete() }
    }
}
