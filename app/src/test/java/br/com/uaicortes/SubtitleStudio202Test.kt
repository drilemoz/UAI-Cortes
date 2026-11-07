package br.com.uaicortes

import br.com.uaicortes.subtitles.core.SubtitleTextCasePolicy
import org.junit.Assert.*
import org.junit.Test

class SubtitleStudio202Test {
    @Test fun curatedPresetsPreserveContentAndTime() {
        assertEquals(8, SubtitleStudioPresets.recommended.size)
        val input=SubtitleCue(id="original",text="Você é o agiota?",startMs=350,endMs=1700,
            speakerId="cliente", wordTimings=listOf(SubtitleWordTiming(350,650)))
        SubtitleStudioPresets.recommended.forEach { preset ->
            val output=SubtitleStyles.applyToCue(input,preset)
            assertEquals(input.text,output.text)
            assertEquals(input.startMs,output.startMs)
            assertEquals(input.endMs,output.endMs)
            assertEquals(input.speakerId,output.speakerId)
            assertEquals(input.wordTimings,output.wordTimings)
            assertTrue(SubtitleStyles.matches(output,preset))
        }
    }
    @Test fun visualCaseOnlyAffectsRenderedFrame() {
        val original=SubtitleCue(id="1",text="Olá, você!",startMs=0,endMs=2000,
            textCase=SubtitleTextCasePolicy.UPPER)
        val frame=SubtitleAnimations.frame(original.copy(animation=SubtitleAnimations.NONE),1000)
        assertEquals("OLÁ, VOCÊ!",frame.text)
        assertEquals("Olá, você!",original.text)
    }
    @Test fun caseInheritedInCopyAndPerCueOverride() {
        val a=SubtitleCue(id="1",text="Oi!",startMs=0,endMs=2000)
        val b=SubtitleCue(id="2",text="Oi!",startMs=2100,endMs=4100)
        val changed=SubtitleStyles.applySelectedToAll(listOf(b),a.copy(textCase="upper"),
            SubtitleStyles.CopyOptions(appearance=true, animation=false)).single()
        assertEquals("upper",changed.textCase)
        assertEquals("Oi!",changed.text)
        assertEquals(b.startMs,changed.startMs)
    }
}
