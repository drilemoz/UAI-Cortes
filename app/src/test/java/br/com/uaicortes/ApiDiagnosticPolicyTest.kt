package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ApiDiagnosticPolicyTest {
    @Test fun parsesOnlyHttpCodesFromKnownStatuses() {
        assertEquals(429,ApiDiagnosticPolicy.httpStatus("erro_429"))
        assertEquals(503,ApiDiagnosticPolicy.httpStatus("503"))
        assertNull(ApiDiagnosticPolicy.httpStatus("erro_secret_key"))
        assertNull(ApiDiagnosticPolicy.httpStatus("0"))
        assertNull(ApiDiagnosticPolicy.httpStatus("erro_999"))
    }
    @Test fun neverIncludesRawUserContentOrCredentials() {
        val log=ApiDiagnosticPolicy.sanitize("gemini-secret", "gerar;token=abc", "gemini-xyz?key=secret",
            503, 800L, null, "my_privacy")
        assertFalse(log.contains("secret"))
        assertFalse(log.contains("token="))
        assertTrue(log.contains("service=unknown"))
        assertTrue(log.contains("category=remote_uncertain"))
        assertTrue(log.contains("tokens=unknown"))
    }
    @Test fun categorizesHttpWithoutBillingClaims() {
        assertEquals("rate_limit",ApiDiagnosticPolicy.category(429))
        assertEquals("auth",ApiDiagnosticPolicy.category(403))
        assertEquals("remote_uncertain",ApiDiagnosticPolicy.category(503))
        assertEquals("not_provided",ApiDiagnosticPolicy.category(null))
    }
}
