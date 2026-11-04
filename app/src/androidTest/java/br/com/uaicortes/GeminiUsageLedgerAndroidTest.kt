package br.com.uaicortes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** O context do APK de testes evita apagar histórico de uma instalação em uso. */
@RunWith(AndroidJUnit4::class)
class GeminiUsageLedgerAndroidTest {
    @Test fun persistsMeasuredAndUnknownRequestsWithoutStoringSensitivePayload() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val ledger = GeminiUsageLedger(context)
        try {
            ledger.clear()
            ledger.record("chave-simulada-so-no-teste", "gemini-teste", "descricao",
                JSONObject().put("usageMetadata", JSONObject()
                    .put("promptTokenCount", 16)
                    .put("candidatesTokenCount", 9)
                    .put("totalTokenCount", 25)))
            ledger.record("chave-simulada-so-no-teste", "gemini-teste", "descricao",
                status = "429")
            val restored = GeminiUsageLedger(context).events()
            assertEquals(2, restored.size)
            assertEquals(25L, restored[0].tokens?.total)
            assertNull(restored[1].tokens)
            assertEquals("429", restored[1].status)
            val prefs = context.getSharedPreferences("gemini_usage_local_v1", android.content.Context.MODE_PRIVATE)
            val raw = prefs.getString("events", "") ?: ""
            assertFalse(raw.contains("chave-simulada-so-no-teste"))
            assertFalse(raw.contains("promptTokenCount"))
            ledger.clear()
            assertTrue(ledger.events().isEmpty())
        } finally {
            ledger.clear()
        }
    }
}
