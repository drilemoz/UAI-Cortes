package br.com.uaicortes

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/** Testes locais do parser: nenhuma requisição de rede ou OAuth real é realizada aqui. */
@RunWith(AndroidJUnit4::class)
class YouTubeGoogleProfile256AndroidTest {
    @Test fun acceptsVerifiedEmailOnly() {
        val json = """{"email":"CONTA@GMAIL.COM","email_verified":true}"""
        assertEquals("conta@gmail.com", YouTubeGoogleProfileApi.parseVerifiedEmail(json))
    }
    @Test fun rejectsUnverifiedAndMalformedEmails() {
        assertThrows(IOException::class.java) {
            YouTubeGoogleProfileApi.parseVerifiedEmail(
                """{"email":"conta@gmail.com","email_verified":false}""")
        }
        assertThrows(IOException::class.java) {
            YouTubeGoogleProfileApi.parseVerifiedEmail(
                """{"email":"invalido","email_verified":true}""")
        }
    }
}
