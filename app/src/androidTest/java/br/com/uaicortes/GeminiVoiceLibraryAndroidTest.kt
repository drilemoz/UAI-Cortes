package br.com.uaicortes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class GeminiVoiceLibraryAndroidTest {
    @Test fun catalogPersistsWithoutNetworkAndRejectsEmptyReplacement() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.cacheDir, "voice-library-test-${UUID.randomUUID()}")
        val preferences = context.getSharedPreferences("voice-library-test-${UUID.randomUUID()}", Context.MODE_PRIVATE)
        try {
            val library = GeminiVoiceLibrary(root, preferences)
            assertTrue(library.load().voices.isEmpty())
            assertFalse(library.autoRefresh()) // Nunca consultar API só ao abrir.
            val sample = GeminiVoice("atendente_test", "Atendente 2", "Voz de teste", "pt-BR")
            assertEquals(1, library.save(listOf(sample)).voices.size)
            assertEquals(sample, GeminiVoiceLibrary(root, preferences).load().voices.single())
            assertThrows(IllegalArgumentException::class.java) { library.save(emptyList()) }
            assertEquals(sample, library.load().voices.single())
            assertTrue(library.setAutoRefresh(true))
            assertTrue(library.autoRefresh())
            assertTrue(library.setLimitMb(50))
            assertEquals(50, library.limitMb())
            assertFalse(library.setLimitMb(0))
        } finally { root.deleteRecursively(); preferences.edit().clear().commit() }
    }
}
