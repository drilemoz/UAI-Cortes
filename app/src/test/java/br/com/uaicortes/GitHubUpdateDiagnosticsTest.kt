package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.json.JSONException

class GitHubUpdateDiagnosticsTest {
    @Test fun excessiveReleaseResponseHasSpecificSafeMessage() {
        val message = GitHubUpdateDiagnostics.exception(GitHubResponseTooLargeException())
        assertTrue(message.contains("limite da consulta"))
    }

    @Test fun dnsAndTimeoutAreDistinguishable() {
        assertTrue(GitHubUpdateDiagnostics.exception(UnknownHostException("secret" )).contains("DNS"))
        assertTrue(GitHubUpdateDiagnostics.exception(SocketTimeoutException("token" )).contains("Tempo limite"))
    }

    @Test fun sslAndConnectionAreDistinguishable() {
        assertTrue(GitHubUpdateDiagnostics.exception(SSLHandshakeException("secret")).contains("TLS/SSL"))
        assertTrue(GitHubUpdateDiagnostics.exception(ConnectException("private")).contains("Conexão"))
    }

    @Test fun privateRepositoryAndRateLimitAreReported() {
        assertTrue(GitHubUpdateDiagnostics.http(404).contains("privado"))
        assertTrue(GitHubUpdateDiagnostics.http(403, "0").contains("limite"))
        assertTrue(GitHubUpdateDiagnostics.http(403, "55").contains("acesso negado"))
        assertTrue(GitHubUpdateDiagnostics.http(429).contains("HTTP 429"))
    }

    @Test fun doesNotLeakExceptionDetails() {
        val secret = "github_pat_super_secreto"
        assertFalse(GitHubUpdateDiagnostics.exception(UnknownHostException(secret)).contains(secret))
        assertFalse(GitHubUpdateDiagnostics.exception(JSONException(secret)).contains(secret))
        assertFalse(GitHubUpdateDiagnostics.exception(IllegalArgumentException(secret)).contains(secret))
    }
}
