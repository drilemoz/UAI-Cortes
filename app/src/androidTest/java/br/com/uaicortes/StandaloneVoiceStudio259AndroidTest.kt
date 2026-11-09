package br.com.uaicortes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Exercita a biblioteca real no armazenamento privado Android. Não usa a API Gemini. */
@RunWith(AndroidJUnit4::class)
class StandaloneVoiceStudio259AndroidTest {
    @Test fun renomearFavoritarPreservaWavELimpaSidecar() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "voice_259_instrumentation")
        directory.deleteRecursively()
        try {
            val library = StandaloneVoiceLibrary(directory)
            val bytes = ByteArray(128) { it.toByte() }
            val file = library.save(GeminiWave(bytes, 3000L),
                StandaloneVoiceLibrary.Info(title="Teste", voiceName="Narradora", excerpt="Olá", style="Neutra"))
            assertEquals(1, library.list().size)
            assertEquals(3000L, library.info(file)?.durationMs)
            assertTrue(library.rename(file, "Abertura do vídeo"))
            assertEquals("Abertura do vídeo", library.info(file)?.title)
            assertTrue(library.toggleFavorite(file))
            assertEquals(true, library.info(file)?.favorite)
            assertArrayEquals(bytes, file.readBytes())
            assertTrue(library.delete(file))
            assertNull(library.info(file))
            assertTrue(library.list().isEmpty())
        } finally { directory.deleteRecursively() }
    }
}
