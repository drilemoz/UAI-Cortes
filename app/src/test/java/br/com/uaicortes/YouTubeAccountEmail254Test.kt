package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class YouTubeAccountEmail254Test {
    @Test fun normalizaContaSemAcessarEmailDoDispositivo() {
        assertEquals("user@gmail.com", YouTubeGoogleAccountEmail.normalized("  USER@GMAIL.COM  "))
        assertTrue(YouTubeGoogleAccountEmail.valid("user@gmail.com"))
        assertTrue(YouTubeGoogleAccountEmail.valid("person+alias@empresa.com.br"))
    }

    @Test fun rejeitaIdentificacoesAmbiguas() {
        listOf("", "semarroba", "a@", "a@b", "a@.com", "a@b..com", "a b@example.com")
            .forEach { assertFalse("E-mail aceito indevidamente: $it", YouTubeGoogleAccountEmail.valid(it)) }
    }
}
