package br.com.uaicortes

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FacebookPageVideoPosts250AndroidTest {
    @Test fun apenasAnexosVideoSaoListadosSemInventarVisualizacoes() {
        val response = JSONObject("""{
          "data":[
            {"id":"12345_99999","message":"Um Reel divertido\nHashtags",
             "created_time":"2026-11-09T01:00:00+0000",
             "permalink_url":"https://www.facebook.com/12345/posts/99999",
             "attachments":{"data":[{"media_type":"video",
               "media":{"image":{"src":"https://scontent.xx.fbcdn.net/sample.jpg"}}}]}}
            },
            {"id":"12345_88888","message":"Foto",
             "attachments":{"data":[{"media_type":"photo"}]}}
          ],"paging":{"next":"https://graph.facebook.com/v26.0/next",
            "cursors":{"after":"QUJDRA=="}}
        }""")
        val result = FacebookPagePostPolicy.parse(response, "12345", "Minha Página")
        assertEquals(2, result.consulted)
        assertEquals(1, result.posts.size)
        assertEquals("Um Reel divertido", result.posts[0].title)
        assertEquals("QUJDRA==", result.nextCursor)
        assertEquals("https://www.facebook.com/12345/posts/99999", result.posts[0].permalink)
    }

    @Test fun bloqueiaLinksExternoESemHttps() {
        assertEquals("", FacebookPagePostPolicy.safePostUrl("https://facebook.com.evil.test/post"))
        assertEquals("", FacebookPagePostPolicy.safePostUrl("http://www.facebook.com/post"))
        assertEquals("", FacebookPagePostPolicy.safeThumbnail("https://notfbcdn.test/anything"))
        assertEquals("", FacebookPagePostPolicy.safeThumbnail("https://evil.fbcdn.net.attacker.test/image"))
        assertThrows(IllegalArgumentException::class.java) {
            FacebookPagePostPolicy.safeCursor("abc\r\nAuthorization:token")
        }
    }

    @Test fun paginaSemVideosOuSemProximaPagina() {
        val result = FacebookPagePostPolicy.parse(
            JSONObject("""{"data":[{"id":"12345_90001","message":"Texto"}]}"""),
            "12345", "Página")
        assertTrue(result.posts.isEmpty())
        assertEquals("", result.nextCursor)
    }
}
