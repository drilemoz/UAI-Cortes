package br.com.uaicortes

import org.junit.Assert.*
import java.io.StringReader
import org.junit.Test

class SubtitleListNavigationTest {
    @Test fun accentAndCaseInsensitiveSearch() {
        assertTrue(SubtitleListNavigation.matches("Vista do Rio São Francisco", "sao francisco"))
        assertTrue(SubtitleListNavigation.matches("Bem-vindo a Pernambuco", "PERNAMBUCO"))
        assertTrue(SubtitleListNavigation.matches("Água correndo", " agua "))
        assertTrue(SubtitleListNavigation.matches("Qualquer legenda", "  "))
        assertFalse(SubtitleListNavigation.matches("Ciclovia", "rodovia"))
    }

    @Test fun navigationStopsAtBothEnds() {
        assertNull(SubtitleListNavigation.adjacentIndex(0,30,-1))
        assertEquals(1,SubtitleListNavigation.adjacentIndex(0,30,1))
        assertEquals(28,SubtitleListNavigation.adjacentIndex(29,30,-1))
        assertNull(SubtitleListNavigation.adjacentIndex(29,30,1))
        assertNull(SubtitleListNavigation.adjacentIndex(-1,30,1))
        assertNull(SubtitleListNavigation.adjacentIndex(0,0,1))
        assertNull(SubtitleListNavigation.adjacentIndex(0,30,0))
    }

    @Test fun newCueFitsTheProjectEvenNearTheEnd() {
        assertNull(SubtitleListNavigation.newInterval(0,null))
        assertEquals(0L to 3000L, SubtitleListNavigation.newInterval(90_000,null))
        assertEquals(89_999L to 90_000L, SubtitleListNavigation.newInterval(90_000,90_000))
        assertEquals(1500L to 1501L, SubtitleListNavigation.newInterval(1501,1500))
        assertEquals(0L to 1L, SubtitleListNavigation.newInterval(1,null))
        assertEquals(0L to 3000L, SubtitleListNavigation.newInterval(90_000,-1))
    }

    @Test fun boundedSrtAcceptsTheExactLimitAndRejectsExtraCharacters() {
        assertEquals("12345",SubtitleListNavigation.readSrtBounded(StringReader("12345"),5))
        assertEquals("",SubtitleListNavigation.readSrtBounded(StringReader(""),5))
        val error=runCatching {
            SubtitleListNavigation.readSrtBounded(StringReader("123456"),5)
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(error!!.message!!.contains("Divida o arquivo"))
    }

}
