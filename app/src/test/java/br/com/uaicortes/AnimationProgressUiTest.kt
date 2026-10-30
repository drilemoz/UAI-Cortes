package br.com.uaicortes

import org.junit.Test
import org.junit.Assert.*

class AnimationProgressUiTest {
    @Test fun nullableProgressAndClamping() {
        assertNull(AnimationProgressUi.fraction(null))
        assertEquals(0f,AnimationProgressUi.fraction(-8)!!,0f)
        assertEquals(0.75f,AnimationProgressUi.fraction(75)!!,0.0001f)
        assertEquals(1f,AnimationProgressUi.fraction(150)!!,0f)
    }
    @Test fun applicationBrandInProgress() {
        assertEquals("Uai Cortes · Exportando vídeo",AnimationProgressUi.title("Exportando vídeo"))
    }
}
