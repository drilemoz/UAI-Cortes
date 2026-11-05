package br.com.uaicortes

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

/** Requer Android real/emulador; prova que decodeStream(null + bounds) não rejeita foto válida. */
class ImageClipImportAndroidTest {
    @Test fun validJpegAndPngAreAcceptedAndCopiedToPrivateStorage() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folder = File(context.filesDir, "project_images_v1").apply { mkdirs() }
        for ((extension, format) in listOf(
            "jpg" to Bitmap.CompressFormat.JPEG,
            "png" to Bitmap.CompressFormat.PNG
        )) {
            val original = File(folder, "probe-${UUID.randomUUID()}.$extension")
            val bitmap = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
            try {
                original.outputStream().use { assertTrue(bitmap.compress(format, 90, it)) }
                val uri = FileProvider.getUriForFile(context,
                    "${context.packageName}.diagnostics", original)
                val inspected = ImageClipImport.inspect(context, uri)
                assertEquals(uri.toString(), inspected.uri)
                assertTrue(inspected.mimeType == "image/jpeg" || inspected.mimeType == "image/png")
                val imported = ImageClipImport.copyIntoProject(context, inspected)
                try {
                    assertTrue(imported.uri != uri.toString())
                    val photo = ImageClipPolicy.create(imported.uri, imported.title,
                        imported.mimeType, 3)
                    assertTrue(photo.isImage)
                    assertEquals(3000L, photo.durationMs)
                    assertNotNull(context.contentResolver.openInputStream(
                        android.net.Uri.parse(imported.uri))?.use { it.read() })
                } finally { ImageClipImport.discardUnused(context, imported) }
            } finally {
                bitmap.recycle()
                original.delete()
            }
        }
    }

    @Test fun corruptJpegIsRejectedWithInvalidImageMessage() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folder = File(context.filesDir, "project_images_v1").apply { mkdirs() }
        val broken = File(folder, "corrupt-${UUID.randomUUID()}.jpg")
        try {
            broken.writeBytes(byteArrayOf(1, 2, 3, 4))
            val uri = FileProvider.getUriForFile(context,
                "${context.packageName}.diagnostics", broken)
            try {
                ImageClipImport.inspect(context, uri)
                fail("Imagem corrompida não pode ser aceita")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("Imagem inválida"))
            }
        } finally { broken.delete() }
    }
}
