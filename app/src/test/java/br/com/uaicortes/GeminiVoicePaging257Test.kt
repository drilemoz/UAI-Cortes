package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiVoicePaging257Test {
    @Test fun fullCatalog() {
        val catalog = (1..500).toList()
        assertEquals(10, GeminiVoicePaging.totalPages(catalog.size))
        assertEquals((1..50).toList(), GeminiVoicePaging.slice(catalog, 0))
        assertEquals((451..500).toList(), GeminiVoicePaging.slice(catalog, 9))
        assertEquals(451 to 500, GeminiVoicePaging.range(catalog.size, 9))
    }
    @Test fun partialAndEmpty() {
        assertEquals(2, GeminiVoicePaging.totalPages(51))
        assertEquals(listOf(51), GeminiVoicePaging.slice((1..51).toList(), 1))
        assertEquals(51 to 51, GeminiVoicePaging.range(51, 1))
        assertTrue(GeminiVoicePaging.slice(emptyList<Int>(), 0).isEmpty())
        assertEquals(0, GeminiVoicePaging.totalPages(0))
        assertEquals(0 to 0, GeminiVoicePaging.range(0, 0))
    }
    @Test fun pageClamped() {
        assertEquals((1..50).toList(), GeminiVoicePaging.slice((1..100).toList(), -99))
        assertEquals((51..100).toList(), GeminiVoicePaging.slice((1..100).toList(), 999))
        assertEquals(51 to 100, GeminiVoicePaging.range(100, 999))
    }
}
