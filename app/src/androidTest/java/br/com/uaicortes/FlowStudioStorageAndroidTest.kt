package br.com.uaicortes

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.UUID

class FlowStudioStorageAndroidTest {
    @Test fun atomicDraftSaveReadAndImagePersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = FlowStudioStorage(context, "instrumented-" + UUID.randomUUID())
        val scene = StudioScene(title = "Cena privada")
        try {
            val draft = FlowStudioDraft(scenes = listOf(scene))
            storage.save(draft)
            assertEquals(draft, storage.read())
            val bitmap = Bitmap.createBitmap(48, 80, Bitmap.Config.ARGB_8888)
            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.toByteArray()
            }
            bitmap.recycle()
            val asset = storage.saveImage(scene.id, bytes)
            assertNotNull(storage.asset(asset))
            assertNull(storage.asset("../../secrets"))
            storage.save(draft.copy(scenes = listOf(scene.copy(imageAsset = asset))))
            assertEquals(asset, storage.read()!!.scenes.first().imageAsset)
        } finally { assertTrue(storage.deleteAll()) }
    }
}
