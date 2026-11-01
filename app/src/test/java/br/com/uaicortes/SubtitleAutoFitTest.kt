package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleAutoFitTest {
    @Test fun shortTextKeepsOriginalSize() {
        assertEquals(1f, SubtitleAutoFit.factor(100f, 150f, 250f), 0.0001f)
    }
    @Test fun longPhraseShrinksInsteadOfTruncating() {
        val factor = SubtitleAutoFit.factor(160f, 1800f, 260f)
        assertTrue(factor < 1f)
        assertTrue(factor >= .48f)
    }
    @Test fun longWordPreventsHorizontalClipping() {
        assertEquals(.5f, SubtitleAutoFit.factor(500f, 500f, 250f), .0001f)
    }
    @Test fun fallbackOnInvalidDimensionsIsSafe() {
        assertEquals(1f, SubtitleAutoFit.factor(100f,1000f,0f), .0001f)
        assertEquals(1f, SubtitleAutoFit.factor(Float.NaN, Float.NaN,100f), .0001f)
    }
}
