package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SocialUploadBatch273Test {
    @Test fun percentageIsBoundedAndUnknownTotalIsNotPretended() {
        val row = UploadReceipt("post", "channel", "Canal", SocialDestination.YOUTUBE,
            UploadPhase.UPLOADING, sent = 250, total = 1000)
        assertEquals(25, row.percent)
        assertEquals(0, row.copy(total = 0).percent)
        assertEquals(100, row.copy(sent = 1200).percent)
        assertEquals(0, row.copy(sent = -1).percent)
    }

    @Test fun estimatorDoesNotReportTimeWithoutConfirmedBytes() {
        val estimator = UploadRateEstimator()
        assertNull(estimator.update(0, 8_000_000, 1000L).second)
        assertNull(estimator.update(0, 8_000_000, 2000L).second)
        val (rate, eta) = estimator.update(1_000_000, 8_000_000, 3000L)
        assertTrue(rate > 0)
        assertNotNull(eta)
        assertTrue(eta!! > 0L)
        assertNull(estimator.update(8_000_000, 8_000_000, 4000L).second)
    }

    @Test fun formattingIsConservative() {
        assertEquals("Calculando tempo restante...", UploadReceiptFormat.eta(null))
        assertEquals("~5 min 2 s restantes", UploadReceiptFormat.eta(302L))
        assertTrue(UploadReceiptFormat.bytes(1_048_576L).contains("1.0 MB"))
    }
}
