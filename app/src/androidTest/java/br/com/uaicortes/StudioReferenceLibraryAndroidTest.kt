package br.com.uaicortes

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** Roda somente em Android CI/emulador ou aparelho, nunca é considerado executado no build local. */
class StudioReferenceLibraryAndroidTest {
    @Test fun reusablePersonAndPlaceSurviveReopenWithGalleryImage() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val isolated = File(app.cacheDir, "studio-library-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(app) { override fun getFilesDir() = isolated }
        val bitmap = Bitmap.createBitmap(40, 72, Bitmap.Config.ARGB_8888)
        val png = ByteArrayOutputStream().use { out ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
            out.toByteArray()
        }
        bitmap.recycle()
        val source = File(isolated, "entrada.png").apply { writeBytes(png) }
        try {
            val referenceLibrary = StudioReferenceLibrary(context)
            val character = StudioPerson(name = "Alienígena", appearance = "quatro olhos")
            val asset = referenceLibrary.importImage(character.id, Uri.fromFile(source))
            assertNotNull(referenceLibrary.asset(asset))
            assertNull(referenceLibrary.asset("../../outro-arquivo"))
            val snapshot = StudioLibraryData(
                people = listOf(character.copy(imageAsset = asset)),
                places = listOf(StudioPlace(name = "Nave espacial")),
                styles = listOf("Animação 3D"))
            referenceLibrary.save(snapshot)
            assertEquals(snapshot, StudioReferenceLibrary(context).read())
            assertEquals(asset, StudioReferenceLibrary(context).read().people.first().imageAsset)
        } finally {
            isolated.deleteRecursively()
        }
    }
}
