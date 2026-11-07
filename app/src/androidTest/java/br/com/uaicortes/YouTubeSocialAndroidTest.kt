package br.com.uaicortes

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YouTubeSocialAndroidTest {
    @Test fun dadosLegadosContinuamCompativeis() {
        val before = SocialAccount(destination = SocialDestination.YOUTUBE, label = "Canal antigo")
        val json = before.toJson()
        json.remove("remoteChannelId")
        assertEquals("", SocialAccount.fromJson(json).remoteChannelId)
        val publication = SocialPublication(accountId = before.id,
            videoUri = "content://media/external/video/media/99", videoName = "teste.mp4")
        val old = publication.toJson()
        old.remove("remoteVideoId")
        assertEquals("", SocialPublication.fromJson(old).remoteVideoId)
        val oldMetrics = SocialMetrics(views = 12).toJson().apply { remove("source") }
        assertEquals("manual", SocialMetrics.fromJson(oldMetrics).source)
    }

    @Test fun vincularCanalExigeYouTubeEIdPublico() {
        val youtube = SocialAccount(destination = SocialDestination.YOUTUBE, label = "YT")
        val page = SocialAccount(destination = SocialDestination.FACEBOOK_PAGE, label = "Página")
        val id = "UC1234567890123456789012"
        assertEquals(id, SocialPublicationPolicy.bindChannel(youtube, id).remoteChannelId)
        assertThrows(IllegalArgumentException::class.java) {
            SocialPublicationPolicy.bindChannel(page, id)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SocialPublicationPolicy.bindChannel(youtube, "https://falso.com")
        }
    }

    @Test fun diagnostico403NaoRevelaRespostaBruta() {
        val apiOff = """{"error":{"errors":[{"reason":"accessNotConfigured"}],"message":"private_project_123"}}"""
        assertTrue(YouTubeSocialApi.errorDescription(403, apiOff).contains("API v3 desativada"))
        assertFalse(YouTubeSocialApi.errorDescription(403, apiOff).contains("private_project_123"))
        val quota = """{"error":{"errors":[{"reason":"quotaExceeded"}]}}"""
        assertTrue(YouTubeSocialApi.errorDescription(403, quota).contains("cota"))
        val disabled = """{"error":{"status":"PERMISSION_DENIED","details":[{"@type":"type.googleapis.com/google.rpc.ErrorInfo","reason":"SERVICE_DISABLED"}]}}"""
        assertTrue(YouTubeSocialApi.errorDescription(403, disabled).contains("API v3 desativada"))
        assertTrue(YouTubeSocialApi.errorDescription(403, "não-json").contains("motivo não identificado"))
    }

    @Test fun uploadNaoRepetidoSemRevisao() {
        val account = SocialAccount(destination = SocialDestination.YOUTUBE, label = "Canal")
        val original = SocialPublication(accountId = account.id,
            videoUri = "content://media/external/video/media/5", videoName = "teste.mp4")
        val review = SocialPublicationPolicy.markUploadReview(original)
        assertEquals(SocialPublicationStatus.UPLOAD_REVIEW, review.status)
        assertThrows(IllegalArgumentException::class.java) { SocialPublicationPolicy.markUploadReview(review) }
        val completed = SocialPublicationPolicy.confirmApiUpload(review, "abcdefghijk")
        assertEquals(SocialPublicationStatus.CONFIRMED_BY_API, completed.status)
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", completed.postUrl)
        assertThrows(IllegalArgumentException::class.java) { SocialPublicationPolicy.markUploadReview(completed) }
        assertThrows(IllegalArgumentException::class.java) {
            SocialPublicationPolicy.confirmApiUpload(review, "inválido")
        }
    }
}
