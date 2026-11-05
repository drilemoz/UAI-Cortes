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

@RunWith(AndroidJUnit4::class)
class SocialStoreAndroidTest {
    private fun temporaryContext(): Context {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val root = File(app.cacheDir, "social-test-${UUID.randomUUID()}").apply { mkdirs() }
        return object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = root
        }
    }

    @Test fun cadastroPersistenteEDestinosIndependentes() {
        val context = temporaryContext()
        val store = SocialStore(context)
        val page = SocialAccount(destination = SocialDestination.FACEBOOK_PAGE, label = "Página teste")
        val profile = SocialAccount(destination = SocialDestination.FACEBOOK_PROFILE, label = "Perfil teste")
        store.save(SocialData(accounts = listOf(page, profile)))
        assertEquals(2, SocialStore(context).load().accounts.size)
        val next = SocialPublicationPolicy.prepare(store.load(), setOf(page.id, profile.id),
            "content://media/external/video/media/123", "video.mp4", "Descrição", "#Teste", now = 42L)
        assertEquals(2, next.publications.size)
        assertEquals(2, next.publications.map { it.accountId }.distinct().size)
        store.save(next)
        assertEquals(SocialPublicationStatus.PREPARED, store.load().publications.first().status)
        assertEquals(2, SocialPublicationPolicy.prepare(store.load(), setOf(page.id,profile.id),
            "content://media/external/video/media/123", "video.mp4", "Nova descrição", "#Novo").publications.size)
    }

    @Test fun abrirCompartilhamentoNaoSignificaPublicado() {
        val account = SocialAccount(destination = SocialDestination.YOUTUBE, label = "Canal")
        val prepared = SocialPublicationPolicy.prepare(SocialData(accounts = listOf(account)), setOf(account.id),
            "content://media/external/video/media/9", "teste.mp4", "descrição", "#hashtag")
        val post = prepared.publications.single()
        assertEquals(SocialPublicationStatus.PREPARED, post.status)
        assertNull(post.metrics.views)
        assertFalse(post.metrics.hasData)
        assertTrue(SocialPublicationPolicy.isSafeHttps("https://youtube.com/watch?v=abc"))
        assertFalse(SocialPublicationPolicy.isSafeHttps("http://youtube.com/watch?v=abc"))
        assertFalse(SocialPublicationPolicy.isShareableVideo("file:///sdcard/segredo.mp4"))
    }

    @Test fun exportacaoNaoIncluiChavesNemHistoricoEImportacaoMescla() {
        val account = SocialAccount(destination = SocialDestination.TIKTOK, label = "Minha conta")
        val data = SocialData(accounts = listOf(account))
        val json = SocialPublicConfig.export(data)
        assertFalse(json.contains("videoUri"))
        assertFalse(json.contains("token"))
        assertFalse(json.contains("secret"))
        assertEquals(1, SocialPublicConfig.merge(data, json.toByteArray()).accounts.size)
        val malicious = """{"format":"uai-social-public-v1","accounts":[],"access_token":"abc"}"""
        assertThrows(IllegalArgumentException::class.java) {
            SocialPublicConfig.merge(data, malicious.toByteArray())
        }
    }

    @Test fun catalogoCorrompidoNaoPodeSerSobrescrito() {
        val context = temporaryContext()
        val store = SocialStore(context)
        store.save(SocialData())
        val source = File(context.filesDir, "social_v1/catalog.json")
        source.writeText("not-json")
        assertThrows(Exception::class.java) { store.save(SocialData()) }
        assertEquals("not-json", source.readText())
    }
}
