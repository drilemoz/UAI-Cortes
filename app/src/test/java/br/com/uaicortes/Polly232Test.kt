package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class Polly232Test {
    private val example = PollyCredentials(
        "AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", "us-east-1")

    @Test fun awsSignatureV4MatchesIndependentReference() {
        val headers = PollySigV4.signedHeaders(example, "GET", "/v1/voices", "LanguageCode=pt-BR",
            byteArrayOf(), Instant.parse("2015-08-30T12:36:00Z"))
        assertEquals("20150830T123600Z", headers["x-amz-date"])
        assertEquals("polly.us-east-1.amazonaws.com", headers["host"])
        assertTrue(headers["authorization"]!!.endsWith(
            "Signature=9f413c5ca0fb074fb157e03ed905c0e752c7cd2eb548e91a97baa010ba58c3d0"))
    }

    @Test fun encodingAndTemporaryTokenAreCanonical() {
        assertEquals("texto%20em%20portugu%C3%AAs", PollySigV4.encode("texto em português"))
        val hdr = PollySigV4.signedHeaders(example.copy(sessionToken = "token+temporary"),
            "POST", "/v1/speech", "", "{}".toByteArray(),
            Instant.parse("2015-08-30T12:36:00Z"), "application/json")
        assertEquals("token+temporary", hdr["x-amz-security-token"])
        assertTrue(hdr["authorization"]!!.contains("x-amz-security-token"))
    }

    @Test fun credentialExportImportWithoutGeminiSettings() {
        assertEquals(example, PollyCredentialCodec.decode(PollyCredentialCodec.encode(example)))
        val text = "AWS_ACCESS_KEY_ID=${example.accessKeyId}\nAWS_SECRET_ACCESS_KEY=${example.secretAccessKey}\nAWS_REGION=us-east-1"
        assertEquals(example, PollyCredentialCodec.decode(text))
        assertThrows(IllegalArgumentException::class.java) {
            PollyCredentialCodec.decode(text.replace("us-east-1", "https://evil.example/"))
        }
    }

    @Test fun characterBillingIsNotGeminiTokens() {
        assertEquals(4, PollyCost.countCharacters("Oi!🙂"))
        assertEquals(0.032, PollyCost.estimateUsd(2000, "neural")!!, 0.0000001)
        assertEquals(0.008, PollyCost.estimateUsd(2000, "standard")!!, 0.0000001)
        assertNull(PollyCost.estimateUsd(2000, "unknown"))
    }
}
