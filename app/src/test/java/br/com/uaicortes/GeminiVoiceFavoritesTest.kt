package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class GeminiVoiceFavoritesTest {
    @Test fun favoritesToggleIsStableAndIdempotent() {
        val start = setOf("pt-br-tutor-7")
        assertEquals(emptySet<String>(), GeminiVoiceFavorites.toggle(start, "pt-br-tutor-7"))
        assertEquals(start + "pt-br-actor-2", GeminiVoiceFavorites.toggle(start, "pt-br-actor-2"))
        assertEquals(start, GeminiVoiceFavorites.toggle(start, ""))
    }

    @Test fun favoritesHaveBoundedCapacity() {
        val current = (1..GeminiVoiceFavorites.MAX_FAVORITES).map { "v$it" }.toSet()
        assertEquals(current, GeminiVoiceFavorites.toggle(current, "another"))
        assertEquals(current - "v5", GeminiVoiceFavorites.toggle(current, "v5"))
    }

    @Test fun batchProgressNeverPretendsToMeasureAnInFlightRequest() {
        assertEquals(0, VoiceIaBatchProgress.percent(0, 1))
        assertEquals(0, VoiceIaBatchProgress.percent(0, 26))
        assertEquals(50, VoiceIaBatchProgress.percent(13, 26))
        assertEquals(100, VoiceIaBatchProgress.percent(26, 26))
        assertEquals(100, VoiceIaBatchProgress.percent(30, 26))
        assertEquals(0, VoiceIaBatchProgress.percent(3, 0))
    }
}
