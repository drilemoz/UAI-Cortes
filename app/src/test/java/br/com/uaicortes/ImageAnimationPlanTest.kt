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

    @Test fun motionPresetsStayWithinBoundsAndRespectDirection() {
        val movements = listOf("auto", "zoom_in", "zoom_out", "horizontal", "vertical")
        for (motion in movements) for (intensity in listOf("suave", "normal", "intensa"))
            for (index in 0..8) for (step in 0..100) {
                val frame = ImageAnimationPlan.frame(index, step / 100f, intensity, motion)
                val margin = (frame.scale - 1f) / frame.scale
                assertTrue(frame.scale >= 1f)
                assertTrue(frame.scale <= 1.15f)
                assertTrue(abs(frame.panX) <= margin * .75f + 1e-4f)
                assertTrue(abs(frame.panY) <= margin * .75f + 1e-4f)
                if (motion == "zoom_in" || motion == "zoom_out") {
                    assertEquals(0f, frame.panX, 0f)
                    assertEquals(0f, frame.panY, 0f)
                }
                if (motion == "horizontal") assertEquals(0f, frame.panY, 0f)
                if (motion == "vertical") assertEquals(0f, frame.panX, 0f)
            }
        assertTrue(ImageAnimationPlan.frame(0, 0f, "normal", "zoom_in").scale <
            ImageAnimationPlan.frame(0, 1f, "normal", "zoom_in").scale)
        assertTrue(ImageAnimationPlan.frame(0, 0f, "normal", "zoom_out").scale >
            ImageAnimationPlan.frame(0, 1f, "normal", "zoom_out").scale)
    }

    @Test fun selectionShowsPhotoOutsideFadeIntervals() {
        for (duration in listOf(3,5,8)) {
            val options=ImageAnimationOptions(secondsPerImage=duration)
            val fade=ImageAnimationPlan.fadeMs(options)
            for (index in 0 until ImageAnimationPlan.MAX_IMAGES) {
                val local=ImageAnimationPlan.focusTimeMs(index,options)%options.durationMs
                assertTrue(local>fade)
                assertTrue(local<options.durationMs-fade)
            }
        }
    }

    @Test fun invalidPresetIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ImageAnimationOptions(motion="falso").checked() }
        assertEquals("auto", ImageAnimationOptions().motion)
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
