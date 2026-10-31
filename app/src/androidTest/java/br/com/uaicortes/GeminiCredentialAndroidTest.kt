package br.com.uaicortes

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import android.content.Context

/** Exige emulador/aparelho com Android Keystore. Não executado sem Android nesta entrega. */
@RunWith(AndroidJUnit4::class)
class GeminiCredentialAndroidTest {
    @Test fun keyIsEncryptedAndRestoresAfterRestart() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = GeminiCredentialStore(context)
        store.remove()
        try {
            val value = "Test_Local_Only_1234567890"
            store.save(value)
            val xml = context.getSharedPreferences("gemini_secret_local", Context.MODE_PRIVATE)
                .getString("ciphertext", "") ?: ""
            assertFalse(xml.contains(value))
            assertTrue(xml.isNotBlank())
            assertEquals(value, GeminiCredentialStore(context).read())
        } finally { store.remove() }
        assertNull(store.read())
    }
    @Test fun savedAudioHasDurableProjectReference() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bytes = ByteArray(44 + 48000)
        val saved = GeminiAudioFiles.save(context, GeminiWave(bytes, 1000L))
        try {
            assertTrue(saved.isFile)
            val track = AudioTrack(GeminiAudioFiles.uri(saved), "Narração", 100L, 1000L, 1f)
            val restored = VideoProject.read(VideoProject(audioTracks=listOf(track)).json())
            assertEquals(track.uri, restored.audioTracks.single().uri)
        } finally { saved.delete() }
    }
}
