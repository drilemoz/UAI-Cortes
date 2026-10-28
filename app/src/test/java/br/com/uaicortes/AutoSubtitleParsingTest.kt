package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class AutoSubtitleParsingTest {
    @Test fun wordTimingFromVosk() {
        val text = """{"result":[{"word":"olá","start":0.25,"end":0.65},{"word":"mundo","start":0.7,"end":1.2}],"text":"olá mundo"}"""
        assertEquals(listOf(Triple("olá",250L,650L),Triple("mundo",700L,1200L)),
            AutoSubtitleEngine.parsedWords(text))
    }
    @Test fun missingWordsDoNotGenerateFakeCaptions() {
        assertTrue(AutoSubtitleEngine.parsedWords("{\"text\":\"\"}").isEmpty())
        assertTrue(AutoSubtitleEngine.parsedWords("invalid json").isEmpty())
    }
}
