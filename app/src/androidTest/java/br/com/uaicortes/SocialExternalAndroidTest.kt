package br.com.uaicortes

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Valida persistência/status sem chamar APIs ou enviar qualquer vídeo. */
@RunWith(AndroidJUnit4::class)
class SocialExternalAndroidTest {
    private fun store(): SocialStore {
        val parent = ApplicationProvider.getApplicationContext<Context>()
        val testDir = File(parent.cacheDir, "social-ext-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(parent) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = testDir
        }
        return SocialStore(context)
    }

    @Test fun vincularPaginaNaoPersisteTokenNemAlteraOutraConta() {
        val store = store()
        val page = SocialAccount(destination = SocialDestination.FACEBOOK_PAGE, label = "Página")
        val personal = SocialAccount(destination = SocialDestination.FACEBOOK_PROFILE, label = "Perfil")
        store.save(SocialData(accounts = listOf(page, personal)))
        val linked = SocialPublicationPolicy.bindPage(page, "123456789012345")
        store.save(SocialData(accounts = listOf(linked, personal)))
        val reloaded = store.load().accounts
        assertEquals("123456789012345", reloaded.first().remotePageId)
        assertTrue(reloaded.last().remotePageId.isBlank())
        assertThrows(IllegalArgumentException::class.java) {
            SocialPublicationPolicy.bindPage(personal, "123456789012345")
        }
        assertFalse(store.load().accounts.first().toJson().toString().contains("access_token"))
    }

    @Test fun resultadoInconclusivoBloqueiaDuplicarERegistroRemotoNaoSignificaPublicado() {
        val store = store()
        val account = SocialAccount(destination = SocialDestination.TIKTOK, label = "Conta")
        val prepared = SocialPublicationPolicy.prepare(SocialData(accounts = listOf(account)),
            setOf(account.id), "content://media/external/video/media/17", "video.mp4", "Teste", "#teste")
        val original = prepared.publications.single()
        val review = SocialPublicationPolicy.markUploadReview(original)
        assertThrows(IllegalArgumentException::class.java) { SocialPublicationPolicy.markUploadReview(review) }
        val submitted = SocialPublicationPolicy.markExternalSubmitted(review, "v_pub_file~v2-1.123456")
        assertEquals(SocialPublicationStatus.REMOTE_SUBMITTED, submitted.status)
        assertTrue(submitted.postUrl.isBlank())
        assertFalse(SocialPublicationPolicy.validVideoId(submitted.remoteVideoId))
        store.save(prepared.copy(publications = listOf(submitted)))
        val reread = store.load().publications.single()
        assertEquals(SocialPublicationStatus.REMOTE_SUBMITTED, reread.status)
        assertEquals(submitted.remoteVideoId, reread.remoteVideoId)
        assertThrows(IllegalArgumentException::class.java) { SocialPublicationPolicy.markUploadReview(reread) }
    }

    @Test fun importacaoLegadaSemIdsRemotosContinuaValida() {
        val store = store()
        val account = SocialAccount(destination = SocialDestination.FACEBOOK_PAGE, label = "Antiga")
        val data = SocialData(accounts = listOf(account))
        store.save(data)
        assertEquals("", store.load().accounts.single().remotePageId)
        val backup = SocialPublicConfig.export(data)
        assertFalse(backup.contains("remotePageId"))
        assertFalse(backup.contains("remoteChannelId"))
        assertFalse(backup.contains("access_token"))
    }
}
