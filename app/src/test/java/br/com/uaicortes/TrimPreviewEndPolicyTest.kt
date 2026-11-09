package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class TrimPreviewEndPolicyTest {
    @Test fun stopsExactlyAtSelectedEnd() {
        assertFalse(TrimPreviewEndPolicy.reachedEnd(3999L, 4000L, false))
        assertTrue(TrimPreviewEndPolicy.reachedEnd(4000L, 4000L, false))
        assertTrue(TrimPreviewEndPolicy.reachedEnd(4200L, 4000L, false))
    }
    @Test fun stopsAtMediaEndAndIgnoresInvalidEnd() {
        assertTrue(TrimPreviewEndPolicy.reachedEnd(900L, 4000L, true))
        assertFalse(TrimPreviewEndPolicy.reachedEnd(0L, 0L, false))
    }
}
