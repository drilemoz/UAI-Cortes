package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class PollyResponseErrorTest {
    @Test fun describeVoicesOnlySendsTheDocumentedNativeLanguageFilter() {
        assertEquals("LanguageCode=pt-BR", PollyApi.voicesQuery(null))
        assertEquals("LanguageCode=pt-BR&NextToken=A%2B%2FB%3D", PollyApi.voicesQuery("A+/B="))
    }

    @Test fun distinguishesParameterRejectionFromIAMWithoutEchoingRemoteMessages() {
        val denied = PollyResponseError.exception("Consultar vozes", 403,
            "AccessDeniedException:https://example.invalid", """{"message":"informação não deve sair"}""")
        assertTrue(denied.message.orEmpty().contains("AccessDeniedException"))
        assertTrue(denied.message.orEmpty().contains("Permissão negada"))
        assertFalse(denied.message.orEmpty().contains("example.invalid"))
        assertFalse(denied.message.orEmpty().contains("informação não deve sair"))

        val invalid = PollyResponseError.exception("Consultar vozes", 400, null,
            """{"__type":"com.amazonaws.polly#ValidationException","Message":"texto bruto"}""")
        assertTrue(invalid.message.orEmpty().contains("ValidationException"))
        assertTrue(invalid.message.orEmpty().contains("parâmetro"))
        assertFalse(invalid.message.orEmpty().contains("texto bruto"))
    }

    @Test fun generic400DoesNotBlameCredentialsAndUnknownValuesStayPrivate() {
        val fallback = PollyResponseError.exception("Consultar vozes", 400, "NÃO_REPRODUZIR",
            """{"Code":"valor desconhecido","Message":"dado sigiloso"}""")
        val message = fallback.message.orEmpty()
        assertTrue(message.contains("HTTP 400"))
        assertTrue(message.contains("não confirma problema na chave"))
        assertFalse(message.contains("NÃO_REPRODUZIR"))
        assertFalse(message.contains("valor desconhecido"))
        assertFalse(message.contains("dado sigiloso"))
    }

    @Test fun tokenAndClockProduceDifferentGuidance() {
        val token = PollyResponseError.exception("Testar", 403, "ExpiredTokenException", null)
        val clock = PollyResponseError.exception("Testar", 400, "RequestTimeTooSkewed", null)
        assertTrue(token.message.orEmpty().contains("Token de sessão"))
        assertTrue(clock.message.orEmpty().contains("data e hora automáticas"))
    }
}
