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

    @Test fun importacaoAutomaticaDoisCanaisSemDuplicarOuPerderHistorico() {
        val first = "UC1234567890123456789012"
        val second = "UC8765432109876543210987"
        val a = SocialPublicationPolicy.upsertYouTube(SocialData(), first, "Primeiro canal")
        val one = a.accounts.single()
        val post = SocialPublication(accountId = one.id,
            videoUri = "content://media/external/video/media/123", videoName = "teste.mp4")
        val withHistory = a.copy(publications = listOf(post))
        val b = SocialPublicationPolicy.upsertYouTube(withHistory, second, "Segundo canal")
        assertEquals(2, b.accounts.size)
        assertEquals(listOf(first, second), b.accounts.map { it.remoteChannelId })
        assertEquals(one.id, b.publications.single().accountId)
        val repeated = SocialPublicationPolicy.upsertYouTube(b, first, "Primeiro alterado")
        assertEquals(2, repeated.accounts.size)
        assertEquals(one.id, repeated.accounts.first().id)
        assertEquals("Primeiro canal", repeated.accounts.first().label)
        assertThrows(IllegalArgumentException::class.java) {
            SocialPublicationPolicy.upsertYouTube(b, "site-falso", "Site")
        }
    }

    @Test fun paginasMetaAutomaticasNaoDuplicamCadastrosManuais() {
        val first = SocialPublicationPolicy.upsertFacebookPage(SocialData(), "1234567890", "Minha página")
        val next = SocialPublicationPolicy.upsertFacebookPage(first, "1234567890", "Nova tentativa")
        assertEquals(1, next.accounts.size)
        assertEquals(first.accounts.single().id, next.accounts.single().id)
        assertEquals(SocialDestination.FACEBOOK_PAGE, next.accounts.single().destination)
        assertFalse(next.accounts.single().toJson().toString().contains("accessToken"))
    }

    @Test fun tituloEditavelDoYouTubeNaoModificaOArquivoOriginalNemCorrompeRegistrosAntigos() {
        val channel = SocialAccount(destination = SocialDestination.YOUTUBE,
            label = "Canal", remoteChannelId = "UC1234567890123456789012")
        val record = SocialPublicationPolicy.prepare(SocialData(accounts = listOf(channel)),
            setOf(channel.id), "content://media/external/video/media/23", "Meu_video.mp4",
            "Descrição", "#Teste", title = "Título escolhido")
            .publications.single()
        assertEquals("Meu_video.mp4", record.videoName)
        assertEquals("Título escolhido", record.title)
        assertEquals("Título escolhido", SocialPublication.fromJson(record.toJson()).title)
        val legacy = record.toJson().apply { remove("title") }
        assertEquals("Meu_video", SocialPublication.fromJson(legacy).title)
        val context = temporaryContext()
        val store = SocialStore(context)
        store.save(SocialData(accounts = listOf(channel), publications = listOf(record)))
        assertEquals("Título escolhido", store.load().publications.single().title)
        assertThrows(IllegalArgumentException::class.java) {
            SocialPublicationPolicy.prepare(SocialData(accounts = listOf(channel)),
                setOf(channel.id), record.videoUri, record.videoName,
                "", "", title = " ")
        }
    }

    @Test fun associacaoGooglePorCanalNaoModificaPublicacoesNemSchemaSocial() {
        val id = "UC1234567890123456789012"
        val context = temporaryContext()
        val store = SocialStore(context)
        val channel = SocialAccount(destination = SocialDestination.YOUTUBE, label = "Canal",
            remoteChannelId = id, googleAccountEmail = "conta@gmail.com")
        val publication = SocialPublication(accountId = channel.id,
            videoUri = "content://media/external/video/media/44", videoName = "video.mp4")
        store.save(SocialData(accounts = listOf(channel), publications = listOf(publication)))
        val read = SocialStore(context).load()
        assertEquals("conta@gmail.com", read.accounts.single().googleAccountEmail)
        assertEquals(publication.id, read.publications.single().id)
        assertTrue(File(context.filesDir, "social_v1/catalog.json").readText().contains("\"schema\":1"))
        assertFalse(SocialPublicConfig.export(read).contains("conta@gmail.com"))
        assertEquals("", SocialPublicationPolicy.bindChannel(channel,
            "UC8765432109876543210987").googleAccountEmail)
        val legacy = channel.toJson().apply { remove("googleAccountEmail") }
        assertEquals("", SocialAccount.fromJson(legacy).googleAccountEmail)
        assertThrows(IllegalArgumentException::class.java) {
            store.save(read.copy(accounts = listOf(channel.copy(googleAccountEmail = "incorreto"))))
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
