package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ImageAnimationPlanTest {
    @Test fun defaultSettingsAndFormats() {
        val defaults=ImageAnimationOptions().checked()
        assertEquals("9:16",defaults.aspect)
        assertEquals(5,defaults.secondsPerImage)
        assertEquals(720,defaults.width)
        assertEquals(1280,defaults.height)
        assertEquals(30,defaults.fps)
        assertEquals(1920,ImageAnimationOptions(quality=1080,aspect="16:9").width)
        assertEquals(720,ImageAnimationOptions(aspect="1:1").height)
    }

    @Test fun smoothCameraWithinFrameBounds() {
        for (intensity in listOf("suave","normal","intensa")) for (i in 0..20) {
            for (step in 0..100) {
                val frame=ImageAnimationPlan.frame(i,step/100f,intensity)
                assertTrue(frame.scale>=1f)
                assertTrue(frame.scale<=1.15f)
                val margin=(frame.scale-1)/frame.scale
                assertTrue(abs(frame.panX)<=margin*.75f+1e-4f)
                assertTrue(abs(frame.panY)<=margin*.75f+1e-4f)
            }
        }
    }

    @Test fun automaticTransitionsAndDuration() {
        assertEquals("Fade branco",ImageAnimationPlan.transitionKind(ImageAnimationOptions(),3))
        assertEquals("Corte direto",ImageAnimationPlan.transitionKind(ImageAnimationOptions(transition="corte"),3))
        assertEquals(15_000L,ImageAnimationPlan.totalDuration(3,ImageAnimationOptions()))
        assertEquals(80*5000L,ImageAnimationPlan.totalDuration(100,ImageAnimationOptions()))
        assertEquals(0f,ImageAnimationPlan.ease(-1f))
        assertEquals(1f,ImageAnimationPlan.ease(2f))
    }
}
