package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class TransitionMotionTest {
    @Test fun motionIsIdentityOutsideTransition() {
        val kinds = listOf(TransitionEffects.ZOOM_IN, TransitionEffects.ZOOM_OUT, TransitionEffects.PAN)
        for (kind in kinds) {
            assertTrue(TransitionEffects.supported(kind))
            assertTrue(TransitionEffects.isMotion(kind))
            assertEquals(1f, TransitionEffects.motionScale(kind, 0f), 0.0001f)
            assertEquals(0f, TransitionEffects.motionX(kind, 0f), 0.0001f)
        }
        assertFalse(TransitionEffects.isMotion(TransitionEffects.CUT))
    }

    @Test fun transformationsRemainWithinSafeBorders() {
        for (kind in listOf(TransitionEffects.ZOOM_IN, TransitionEffects.ZOOM_OUT, TransitionEffects.PAN)) {
            for (step in 0..100) {
                val strength = step / 100f
                val scale = TransitionEffects.motionScale(kind, strength)
                val offset = TransitionEffects.motionX(kind, strength)
                assertTrue(scale >= 1f)
                assertTrue(scale < 1.15f)
                assertTrue(kotlin.math.abs(offset) <= (scale - 1f) + 0.0001f)
            }
        }
    }
}
