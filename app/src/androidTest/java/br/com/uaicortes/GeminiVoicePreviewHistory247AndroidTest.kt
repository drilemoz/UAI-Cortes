package br.com.uaicortes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/** Exercitar a biblioteca real no armazenamento privado do Android, sem rede nem API Gemini. */
@RunWith(AndroidJUnit4::class)
class GeminiVoicePreviewHistory247AndroidTest {
    private fun wave(): ByteArray {
        val n = 24000
        return ByteBuffer.allocate(44 + n).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + n); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(24000); putInt(48000); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(n); repeat(n / 2) { putShort(0) }
        }.array()
    }
    @Test fun differentPhrasesForSameVoiceStayAccessibleAndDeleteIsSelective() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cache = GeminiVoicePreviewCache(ctx)
        val voiceId = "test-247-${UUID.randomUUID()}"
        val otherVoice = "other-247-${UUID.randomUUID()}"
        val a = GeminiPreviewIdentity.idForText("", GeminiModels.LITE, voiceId, "Neutra", "Olá, bom dia!")
        val b = GeminiPreviewIdentity.idForText("", GeminiModels.LITE, voiceId, "Neutra", "Outra frase.")
        val c = GeminiPreviewIdentity.idForText("", GeminiModels.LITE, otherVoice, "Neutra", "Outra frase.")
        assertNotEquals(a, b)
        try {
            cache.save(a, wave(), voiceId)
            cache.save(b, wave(), voiceId)
            cache.save(c, wave(), otherVoice)
            assertEquals(2, cache.listByVoice(voiceId).size)
            assertEquals(2, cache.countsByVoice()[voiceId])
            assertEquals(1, cache.listByVoice(otherVoice).size)
            assertTrue(cache.removeOne(a))
            assertNull(cache.find(a))
            assertNotNull(cache.find(b))
            assertNotNull(cache.find(c))
            assertEquals(1, cache.listByVoice(voiceId).size)
            assertFalse(cache.removeOne(a))
        } finally {
            cache.removeOne(a); cache.removeOne(b); cache.removeOne(c)
        }
    }
}
