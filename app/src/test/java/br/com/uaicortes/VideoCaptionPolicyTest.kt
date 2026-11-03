package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class VideoCaptionPolicyTest {
    @Test fun generatedCaptionRespectsOneHundredCharactersAndFiveHashtags() {
        val result = VideoCaptionPolicy.fromModel(
            "Uma situação inesperada que deixou todo mundo surpreso e gerou uma reação engraçada de verdade",
            listOf("#UaiCortes", "#Humor", "#Surpresa", "#Reacao", "#VideoCurto"))!!
        assertTrue(result.combined.length <= 100)
        assertEquals(5, VideoCaptionPolicy.hashtagCount(result.hashtags))
        assertTrue(VideoCaptionPolicy.isValid(result.description, result.hashtags))
        assertEquals(result.description + "\n" + result.hashtags, result.combined)
    }

    @Test fun refusesWrongHashtagCountAndDuplicates() {
        assertNull(VideoCaptionPolicy.fromModel("Um vídeo", listOf("#Uma", "#Duas")))
        assertNull(VideoCaptionPolicy.fromModel("Um vídeo", listOf("#Uma", "#Duas", "#Tres", "#Quatro", "#UMA")))
        assertFalse(VideoCaptionPolicy.isValid("Um vídeo", "#Uma #Duas #Tres #Quatro"))
    }

    @Test fun refusesEmojiEmoticonsInvalidTagsAndOverLimit() {
        assertNull(VideoCaptionPolicy.fromModel("Olha só 😀", listOf("#A", "#B", "#C", "#D", "#E")))
        assertNull(VideoCaptionPolicy.fromModel("Bom vídeo", listOf("#A", "#B", "#C", "#D", "#Tag-ruim")))
        assertFalse(VideoCaptionPolicy.isValid("Eu gostei :)", "#A #B #C #D #E"))
        assertFalse(VideoCaptionPolicy.isValid("a".repeat(101), "#A #B #C #D #E"))
        assertFalse(VideoCaptionPolicy.isValid("A\nB", "#A #B #C #D #E"))
    }

    @Test fun fiveLongTagsCannotEliminateDescriptionBudget() {
        val tags = listOf("#" + "a".repeat(24), "#" + "b".repeat(24),
            "#" + "c".repeat(24), "#" + "d".repeat(24), "#" + "e".repeat(24))
        assertNull(VideoCaptionPolicy.fromModel("Teste", tags))
    }
}
