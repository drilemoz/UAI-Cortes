package br.com.uaicortes

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Exige emulador/aparelho: a URI criada deve continuar acessível após apagar Downloads. */
@RunWith(AndroidJUnit4::class)
class ProjectMediaCopiesAndroidTest {
    @Test fun videoCopyRemainsReadableAfterOriginalIsRemoved() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val assetContext = InstrumentationRegistry.getInstrumentation().context
        val original = File(context.cacheDir, "media-copy-${UUID.randomUUID()}.mp4")
        assetContext.assets.open("export_smoke_16x9.mp4").use { src ->
            original.outputStream().use { dest -> src.copyTo(dest) }
        }
        val expected = original.length()
        var safe: Uri? = null
        try {
            val source = Uri.fromFile(original)
            val projectId = UUID.randomUUID().toString()
            safe = ProjectMediaCopies.copy(context, source, projectId, ProjectMediaCopies.Kind.VIDEO)
            assertNotEquals(source, safe)
            // Novos backups são internos: nunca inserir no MediaStore/Galeria.
            assertEquals("${context.packageName}.diagnostics", safe.authority)
            assertEquals("project_media", safe.pathSegments.first())
            assertEquals(projectId, safe.pathSegments[1])
            assertEquals("video", safe.pathSegments[2])
            assertTrue(ProjectMediaAvailability.canOpen(context, safe.toString()))
            // A cópia é independente do caminho inicial; não depende do cache/Downloads.
            assertTrue(original.delete())
            val actual = context.contentResolver.openInputStream(safe)?.use { input ->
                input.copyTo(java.io.ByteArrayOutputStream())
            }
            assertEquals(expected, actual)
            val second = ProjectMediaCopies.copy(context, safe, projectId, ProjectMediaCopies.Kind.VIDEO)
            assertEquals(safe, second)
        } finally {
            original.delete()
            safe?.let { uri ->
                if (uri.authority == "${context.packageName}.diagnostics")
                    runCatching { context.contentResolver.delete(uri, null, null) }
            }
        }
    }
}
