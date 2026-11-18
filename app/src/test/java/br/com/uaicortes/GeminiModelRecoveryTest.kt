package br.com.uaicortes

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class GeminiModelRecoveryTest {
    private val flash = "gemini-3.5-flash"
    private fun http(code: Int, modelNotFound: Boolean = false) =
        GeminiHttpException(code, "body with private project and secret-value", modelNotFound)

    @Test fun unavailableTextUsesOneAlternateAndReportsActualModel() = runBlocking {
        val trace = mutableListOf<GeminiRecoveryTrace>()
        val calls = mutableListOf<Pair<String, Boolean>>()
        val answer = withContext(GeminiRecoveryChannel { trace += it }) {
            GeminiModelRecovery.execute(GeminiRecoveryRoute.PROOFREAD, flash, pause = {}) { model, first ->
                calls += model to first
                if (first) throw http(503)
                "validated"
            }
        }
        assertEquals("validated", answer)
        assertEquals(listOf(flash to true, GeminiModelRecovery.TEXT_LITE to false), calls)
        assertTrue(trace.first().status!!.contains("Tentando outro modelo"))
        assertFalse(trace.last().failed)
        assertTrue(trace.last().status!!.contains(GeminiModelRecovery.TEXT_LITE))
    }

    @Test fun secondFailureProducesExportableRedactedReportWithoutThirdRequest() = runBlocking {
        val trace = mutableListOf<GeminiRecoveryTrace>()
        var calls = 0
        try {
            withContext(GeminiRecoveryChannel { trace += it }) {
                GeminiModelRecovery.execute(GeminiRecoveryRoute.DESCRIPTION, "gemini-my-private-id", pause = {}) { _, first ->
                    calls++
                    throw if (first) http(503) else http(429)
                }
            }
            fail("A segunda falha precisa chegar ao chamador")
        } catch (expected: GeminiHttpException) { assertEquals(429, expected.statusCode) }
        assertEquals(2, calls)
        val report = trace.last().report("0.1.311")
        assertTrue(trace.last().failed)
        assertTrue("HTTP 503" in report && "HTTP 429" in report && "0.1.311" in report)
        assertFalse("secret-value" in report || "my-private-id" in report)
    }

    @Test fun timeoutQuotaBadResponseAndCancelNeverStartAnotherRequest() = runBlocking {
        val faults = listOf(
            SocketTimeoutException(), IOException("wrapped timeout", SocketTimeoutException()),
            http(429), http(403), http(400), http(404), http(500), GeminiInvalidResult("invalid"),
            CancellationException("cancel")
        )
        for (failure in faults) {
            var attempts = 0
            try {
                GeminiModelRecovery.execute(GeminiRecoveryRoute.SCRIPT, flash, pause = {}) { _, _ ->
                    attempts++
                    throw failure
                }
            } catch (actual: Exception) { assertSame(failure, actual) }
            assertEquals(failure.javaClass.simpleName, 1, attempts)
        }
    }

    @Test fun cancellationDuringDelayDoesNotSubmitTheAlternative() = runBlocking {
        var attempts = 0
        try {
            GeminiModelRecovery.execute(GeminiRecoveryRoute.VISUAL, flash,
                pause = { throw CancellationException("stopped") }) { _, _ ->
                attempts++
                throw http(503)
            }
        } catch (_: CancellationException) { }
        assertEquals(1, attempts)
    }

    @Test fun batchVoiceAndTimestampedSpeechHaveDifferentRules() {
        val unavailable = GeminiRecoveryFault.from(http(503))
        val missingModel = GeminiRecoveryFault.from(http(404, modelNotFound = true))
        assertNull(GeminiModelRecovery.alternate(GeminiRecoveryRoute.BATCH, flash, unavailable))
        assertEquals(GeminiModelRecovery.TEXT_LITE,
            GeminiModelRecovery.alternate(GeminiRecoveryRoute.BATCH, flash, missingModel))
        assertNull(GeminiModelRecovery.alternate(GeminiRecoveryRoute.TRANSCRIBE, flash, missingModel))
        assertEquals(GeminiModels.LITE,
            GeminiModelRecovery.alternate(GeminiRecoveryRoute.VOICE, GeminiModels.FLASH, unavailable))
        assertNull(GeminiModelRecovery.alternate(GeminiRecoveryRoute.VOICE, GeminiModels.LITE, unavailable))
    }

    @Test fun exactModelPreviewAndPersistentQueuesDoNotSwitch() = runBlocking {
        var attempts = 0
        try {
            GeminiModelRecovery.execute(GeminiRecoveryRoute.VOICE, GeminiModels.FLASH,
                allowAlternate = false, pause = {}) { _, _ ->
                attempts++
                throw http(503)
            }
        } catch (_: GeminiHttpException) { }
        assertEquals(1, attempts)
    }
}
