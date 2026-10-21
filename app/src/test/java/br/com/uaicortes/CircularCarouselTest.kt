package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class CircularCarouselTest {
    @Test fun circulaNosDoisSentidos() {
        val size = 7
        val middle = CircularCarousel.startIndex(size)
        assertEquals(0, CircularCarousel.optionIndex(middle,size))
        for (offset in -40..40) {
            assertEquals(Math.floorMod(offset, size),
                CircularCarousel.optionIndex(middle+offset,size))
        }
    }

    @Test fun limitesNaoEstouram() {
        for (size in 1..20) {
            assertTrue(CircularCarousel.startIndex(size) > 1000000)
            assertEquals((Int.MAX_VALUE-1)%size,
                CircularCarousel.optionIndex(Int.MAX_VALUE-1,size))
        }
    }

    @Test(expected=IllegalArgumentException::class)
    fun rejeitaCarrosselVazio() { CircularCarousel.startIndex(0) }
}
