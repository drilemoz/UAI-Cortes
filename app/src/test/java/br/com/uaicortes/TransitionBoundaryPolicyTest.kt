package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class TransitionBoundaryPolicyTest {
    @Test fun boundariesAreZeroBasedAndNeverExceedClipCount() {
        assertEquals(0, TransitionBoundaryPolicy.count(0))
        assertEquals(0, TransitionBoundaryPolicy.count(1))
        assertEquals(1, TransitionBoundaryPolicy.count(2))
        assertEquals(2, TransitionBoundaryPolicy.count(3))
        assertEquals(0, TransitionBoundaryPolicy.clamp(999, 2))
        assertEquals(1, TransitionBoundaryPolicy.clamp(999, 3))
        assertEquals(0, TransitionBoundaryPolicy.clamp(-1, 3))
        assertFalse(TransitionBoundaryPolicy.isValid(1, 2))
        assertTrue(TransitionBoundaryPolicy.isValid(0, 2))
        assertTrue(TransitionBoundaryPolicy.isValid(1, 3))
    }
}
