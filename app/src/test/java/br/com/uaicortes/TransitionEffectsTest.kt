package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class TransitionEffectsTest {
    @Test fun allKindsProduceFiniteMatrices() {
        for (kind in TransitionEffects.names) {
            for (strength in listOf(-1f,0f,.15f,.5f,.75f,1f,2f)) {
                val matrix=TransitionEffects.matrix(kind,strength)
                assertEquals(16,matrix.size)
                assertTrue("$kind $strength",matrix.all { it.isFinite() })
                assertEquals(1f,matrix[15],.0001f)
                assertEquals(20,TransitionEffects.composeValues(matrix).size)
            }
        }
    }
    @Test fun nothingIsAppliedOutsideTransitions() {
        val identity=TransitionEffects.matrix(TransitionEffects.CUT,0f)
        for (kind in TransitionEffects.names) {
            assertArrayEquals(identity,TransitionEffects.matrix(kind,0f),.0001f)
            val elsewhere=TransitionEffects.matrixAt(500,1000,kind,200,kind,200)
            assertArrayEquals(identity,elsewhere,.0001f)
        }
    }
    @Test fun flashIsConcentratedNearTheBoundary() {
        assertEquals(0f,TransitionEffects.intensity(TransitionEffects.FLASH_WHITE,0f),0f)
        assertTrue(TransitionEffects.intensity(TransitionEffects.FLASH_WHITE,.5f) < .05f)
        assertEquals(1f,TransitionEffects.intensity(TransitionEffects.FLASH_WHITE,1f),0f)
    }
    @Test fun previewAndExporterHaveTheSamePixelCoefficients() {
        val matrix=TransitionEffects.matrix(TransitionEffects.GOLD,.5f)
        val compose=TransitionEffects.composeValues(matrix)
        assertEquals(matrix[0],compose[0],0f)
        assertEquals(matrix[4],compose[1],0f)
        assertEquals(matrix[12]*255f,compose[4],0f)
        assertEquals(matrix[13]*255f,compose[9],0f)
    }
    @Test fun legacyFadeColorsMatchBaseIntent() {
        val white=TransitionEffects.matrix(TransitionEffects.WHITE,1f)
        val black=TransitionEffects.matrix(TransitionEffects.BLACK,1f)
        assertEquals(1f,white[12],.0001f)
        assertEquals(0f,black[12],.0001f)
        assertEquals(0f,black[0],.0001f)
        assertEquals(1f,TransitionEffects.matrix(TransitionEffects.CUT,1f)[0],.0001f)
    }
}
