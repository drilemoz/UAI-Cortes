package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ElevenLabs272Test {
    @Test fun keyIsValidatedWithoutLoggingOrHeaderInjection() {
        assertEquals("sk_example_123456789", ElevenLabsKeyPolicy.validate(" sk_example_123456789 "))
        assertThrows(IllegalArgumentException::class.java) {
            ElevenLabsKeyPolicy.validate("sk_example\r\nHeader: x")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ElevenLabsKeyPolicy.validate("invalid key with spaces")
        }
    }

    @Test fun acceptsPlainEnvAndJsonKeyImports() {
        val key = "sk_example_123456789"
        assertEquals(key, ElevenLabsKeyPolicy.importText(key))
        assertEquals(key, ElevenLabsKeyPolicy.importText("# nota\nELEVENLABS_API_KEY=$key\n"))
        assertEquals(key, ElevenLabsKeyPolicy.importText("{\"ELEVENLABS_API_KEY\":\"$key\"}"))
        assertThrows(IllegalArgumentException::class.java) {
            ElevenLabsKeyPolicy.importText("GEMINI_API_KEY=$key")
        }
    }

    @Test fun parsesPagedVoicesWithoutInventingLanguage() {
        val page = ElevenLabsApi.parseVoices("""{
            "voices":[
                {"voice_id":"abc1234567","name":"Voz A","category":"premade","labels":{"language":"pt"}},
                {"voice_id":"def1234567","name":"Voz B"},
                {"voice_id":"","name":"Inválida"}],
            "has_more":true,"next_page_token":"next123"
        }""")
        assertEquals(2, page.voices.size)
        assertEquals("pt", page.voices.first().language)
        assertNull(page.voices[1].language)
        assertEquals("next123", page.nextPageToken)
        assertNull(ElevenLabsApi.parseVoices("""{"voices":[],"has_more":false}""").nextPageToken)
    }

    @Test fun handlesAccountWithoutInventingCredits() {
        val account = ElevenLabsApi.parseAccount("""{"user_id":"user_1","subscription":{"tier":"free","character_count":12,"character_limit":100}}""")
        assertEquals("free", account.tier)
        assertEquals(88L, account.remainingCredits)
        assertNull(ElevenLabsApi.parseAccount("""{"user_id":"user_1","subscription":{"tier":"free"}}""").remainingCredits)
    }

    @Test fun safeHttpStatusExplanations() {
        assertTrue(ElevenLabsApi.statusMessage(403).contains("permissões"))
        assertTrue(ElevenLabsApi.statusMessage(429).contains("créditos"))
        assertFalse(ElevenLabsApi.statusMessage(401).contains("sk_example"))
    }
}
