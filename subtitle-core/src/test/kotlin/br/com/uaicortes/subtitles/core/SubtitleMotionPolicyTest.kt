package br.com.uaicortes.subtitles.core

import org.junit.Assert.*
import org.junit.Test

class SubtitleMotionPolicyTest {
    @Test fun noMotionForUnknownLegacyValue() {
        assertEquals(SubtitleMotionPolicy.Motion(), SubtitleMotionPolicy.evaluate("old_unknown", 100, 1000))
    }

    @Test fun smoothEffectsAreFiniteBoundedAndEndInvisible() {
        for (style in SubtitleMotionPolicy.styles) {
            for (duration in listOf(1L, 33L, 120L, 500L, 4000L, 20000L)) {
                for (speed in listOf(.5f, 1f, 2f, Float.NaN, Float.POSITIVE_INFINITY)) {
                    for (fraction in 0..40) {
                        val at = duration * fraction / 40L
                        val frame = SubtitleMotionPolicy.evaluate(style, at, duration, speed)
                        assertTrue("$style $duration: alpha=${frame.alpha}", frame.alpha.isFinite() && frame.alpha in 0f..1f)
                        assertTrue("$style $duration: scale=${frame.scale}", frame.scale.isFinite() && frame.scale in .8f..1.16f)
                        assertTrue(frame.slideX.isFinite() && frame.slideX in -.1f.. .1f)
                        assertTrue(frame.slideY.isFinite() && frame.slideY in -.04f.. .04f)
                    }
                }
                assertEquals(0f, SubtitleMotionPolicy.evaluate(style, 0, duration).alpha, .00001f)
                assertEquals(0f, SubtitleMotionPolicy.evaluate(style, duration, duration).alpha, .00001f)
            }
        }
    }

    @Test fun curvesAreTimeBasedAndDoNotNeedState() {
        for (style in SubtitleMotionPolicy.styles) {
            val first = SubtitleMotionPolicy.evaluate(style, 125L, 2000L)
            SubtitleMotionPolicy.evaluate(style, 1700L, 2000L)
            assertEquals(first, SubtitleMotionPolicy.evaluate(style, 125L, 2000L))
            assertEquals(first, SubtitleMotionPolicy.evaluate(style, 125L, 2000L, 1f))
        }
    }

    @Test fun speedAndTimeClampingArePredictable() {
        val normal = SubtitleMotionPolicy.evaluate(SubtitleMotionPolicy.CINEMATIC, 180L, 2000L, 1f)
        val fast = SubtitleMotionPolicy.evaluate(SubtitleMotionPolicy.CINEMATIC, 180L, 2000L, 2f)
        assertTrue(fast.slideY < normal.slideY)
        val start = SubtitleMotionPolicy.evaluate(SubtitleMotionPolicy.CINEMATIC, 0L, 2000L)
        assertEquals(start, SubtitleMotionPolicy.evaluate(SubtitleMotionPolicy.CINEMATIC, -100L, 2000L))
        assertEquals(0f, SubtitleMotionPolicy.evaluate(SubtitleMotionPolicy.CINEMATIC, 5000L, 2000L).alpha, 0f)
    }

    @Test fun stylesAreVisuallyDistinct() {
        val frames = SubtitleMotionPolicy.styles.map { SubtitleMotionPolicy.evaluate(it, 60, 1000) }
        assertEquals(SubtitleMotionPolicy.styles.size, frames.distinct().size)
    }
}
