package br.com.uaicortes.subtitles.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleTextCasePolicyTest {
    @Test fun portugueseAccentsAndPunctuation() {
        val text = "Você que é o agiota? Agora... vamos!"
        assertEquals("VOCÊ QUE É O AGIOTA? AGORA... VAMOS!", SubtitleTextCasePolicy.display(text, "upper"))
        assertEquals("você que é o agiota? agora... vamos!", SubtitleTextCasePolicy.display(text, "lower"))
        assertEquals(text, SubtitleTextCasePolicy.display(text, "original"))
    }
    @Test fun unknownLegacyValueRemainsOriginal() {
        assertEquals("Não mude!", SubtitleTextCasePolicy.display("Não mude!", "legacy_unknown"))
        assertEquals(SubtitleTextCasePolicy.ORIGINAL, SubtitleTextCasePolicy.normalize(null))
    }
    @Test fun emptyStringDoesNotCrash() {
        assertEquals("", SubtitleTextCasePolicy.display("", "upper"))
    }
}
