package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ModelTransferProgressTest {
    @Test fun zeroPercentDoesNotHideTransferredBytes() {
        assertEquals(0, ModelTransferProgress.percent(100_000, 31_000_000))
        assertEquals("97 KiB", ModelTransferProgress.bytesLabel(100_000))
    }

    @Test fun unknownLengthIsIndeterminate() {
        assertNull(ModelTransferProgress.percent(150_000, null))
        assertNull(ModelTransferProgress.percent(150_000, 0))
    }

    @Test fun percentageIsBounded() {
        assertEquals(100, ModelTransferProgress.percent(40_000_000, 31_000_000))
        assertEquals(0, ModelTransferProgress.percent(-10, 31_000_000))
    }
}
