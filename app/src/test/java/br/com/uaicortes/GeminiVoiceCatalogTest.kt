package br.com.uaicortes

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class GeminiVoiceCatalogTest {
    private fun page(vararg ids: String, token: String = ""): JSONObject = JSONObject().apply {
        val voices = org.json.JSONArray()
        ids.forEach { voices.put(JSONObject().put("id", it).put("display_name", "Voz $it")
            .put("language_code", "pt-BR")) }
        put("voices", voices)
        if (token.isNotEmpty()) put("next_page_token", token)
    }

    @Test fun filteredResponseDoesNotTriggerUnfilteredRequest() = runBlocking {
        val seen = mutableListOf<String>()
        val result = GeminiVoiceCatalog.fetchCatalog {
            seen += it
            page("pt-br-advisor-4", "pt-br-narrator-2")
        }
        assertEquals(2, result.voices.size)
        assertEquals(1, seen.size)
        assertTrue(seen.single().contains("language_code=pt-BR"))
        assertTrue(result.report.single().contains("2 voz(es)"))
    }

    @Test fun emptyFilteredSearchFallsBackAndKeepsManualSelection() = runBlocking {
        val seen = mutableListOf<String>()
        val result = GeminiVoiceCatalog.fetchCatalog { path ->
            seen += path
            if (path.contains("language_code")) page() else page("Puck", "Charon")
        }
        assertEquals(listOf("Puck", "Charon"), result.voices.map { it.id })
        assertEquals(2, seen.size)
        assertFalse(seen.last().contains("language_code"))
        assertTrue(result.report.last().contains("idioma não presumido"))
    }

    @Test fun rejectedLanguageFilterRetriesUnfilteredOnlyOnBadRequest() = runBlocking {
        val seen = mutableListOf<String>()
        val result = GeminiVoiceCatalog.fetchCatalog { path ->
            seen += path
            if (path.contains("language_code")) throw GeminiApiError(400, "INVALID_ARGUMENT")
            page("Puck")
        }
        assertEquals(1, result.voices.size)
        assertEquals(2, seen.size)
        assertTrue(result.report.joinToString().contains("HTTP 400"))
    }

    @Test fun authenticationFailureDoesNotRetryAndDoesNotLeakCredentials() = runBlocking {
        var calls = 0
        val secret = "API_KEY_DO_NOT_EXPOSE"
        try {
            GeminiVoiceCatalog.fetchCatalog {
                calls++
                throw GeminiApiError(403, "PERMISSION_DENIED")
            }
            fail("Deveria lançar erro")
        } catch (e: GeminiVoiceCatalogFailure) {
            assertEquals(1, calls)
            assertEquals("Filtro pt-BR", e.stage)
            assertTrue(e.message.orEmpty().contains("403"))
            assertFalse(e.report().joinToString().contains(secret))
        }
    }

    @Test fun paginationUsesSameLanguageFilterAndEncodesToken() = runBlocking {
        val paths = mutableListOf<String>()
        val result = GeminiVoiceCatalog.fetchCatalog { path ->
            paths += path
            if (paths.size == 1) page("voice_1", token = "pagina +&")
            else page("voice_1", "voice_2")
        }
        assertEquals(listOf("voice_1", "voice_2"), result.voices.map { it.id })
        assertEquals(2, paths.size)
        assertTrue(paths.last().contains("language_code=pt-BR"))
        assertTrue(paths.last().contains("page_token=pagina+%2B%26"))
    }

    @Test fun emptyApiResponseIsReportedRatherThanSuccess() = runBlocking {
        val result = GeminiVoiceCatalog.fetchCatalog { JSONObject() }
        assertTrue(result.voices.isEmpty())
        assertTrue(result.report.last().contains("sem vozes"))
    }

    @Test fun malformedRowsAreDiscardedAndMetadataIsPreserved() {
        val input = JSONObject("""{"voices":[{"id":""},{"id":"pt-br-a","display_name":"Narradora","language_code":"pt-BR","description":"Informativa"},{"display_name":"sem id"},null]}""")
        val parsed = GeminiVoiceCatalog.parsePage(input)
        assertEquals(1, parsed.voices.size)
        assertEquals("pt-br-a", parsed.voices[0].id)
        assertEquals("Narradora", parsed.voices[0].name)
        assertEquals("pt-BR", parsed.voices[0].languageCode)
    }

    @Test fun safeDiagnosticNeverIncludesRawErrorPayload() {
        assertEquals("Erro de rede ou resposta inesperada.",
            GeminiVoiceCatalog.safeMessage(IOException("key=SECRET&user=SECRET")))
        assertTrue(GeminiVoiceCatalog.safeMessage(GeminiApiError(429, "RESOURCE_EXHAUSTED")).contains("429"))
        assertEquals("Sem acesso ao servidor (DNS/rede).",
            GeminiVoiceCatalog.safeMessage(java.net.UnknownHostException("key=SECRET")))
    }
}
