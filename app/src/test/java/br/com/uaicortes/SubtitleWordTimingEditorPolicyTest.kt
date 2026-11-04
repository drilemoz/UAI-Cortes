package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleWordTimingEditorPolicyTest {
    private fun cue(text: String = "Uai que coisa boa", start: Long = 1000L,
        end: Long = 3000L) = SubtitleCue(id="manual",text=text,startMs=start,endMs=end,
            animation=SubtitleAnimations.KARAOKE)

    @Test fun explicitDistributionIsContinuousAndWithinSubtitle() {
        val c=cue()
        val times=SubtitleWordTimingEditorPolicy.distribute(c)
        assertEquals(4,times.size)
        assertEquals(1000L,times.first().startMs)
        assertEquals(3000L,times.last().endMs)
        assertTrue(SubtitleWordTimingEditorPolicy.valid(c,times))
        assertEquals(times[0].endMs,times[1].startMs)
    }

    @Test fun editOneBoundaryPreservesOthersAndRejectsInvalid() {
        val c=cue()
        val base=SubtitleWordTimingEditorPolicy.distribute(c)
        val changed=SubtitleWordTimingEditorPolicy.moveBoundary(c,base,1,2100L)!!
        assertEquals(base[0],changed[0])
        assertEquals(2100L,changed[1].endMs)
        assertEquals(2100L,changed[2].startMs)
        assertEquals(base[3],changed[3])
        assertTrue(SubtitleWordTimingEditorPolicy.valid(c,changed))
        assertNull(SubtitleWordTimingEditorPolicy.moveBoundary(c,base,1,base[1].startMs))
        assertNull(SubtitleWordTimingEditorPolicy.moveBoundary(c,base,99,1500L))
    }

    @Test fun recognizesPausesWithoutMakingUpAudioTimes() {
        val c=cue(text="Uai demais",start=1000L,end=3000L)
        val gaps=listOf(SubtitleWordTiming(1200L,1400L),SubtitleWordTiming(2200L,2700L))
        assertTrue(SubtitleWordTimingEditorPolicy.valid(c,gaps))
        val preview=SubtitleWordTimingEditorPolicy.previewCue(c.copy(wordTimings=gaps),3000L)
        assertEquals(0L,preview.startMs)
        assertEquals(3000L,preview.endMs)
        assertEquals(listOf(SubtitleWordTiming(300L,600L),
            SubtitleWordTiming(1800L,2550L)),preview.wordTimings)
        assertEquals(SubtitleWordEffects.wordRanges(preview.text)[1],
            SubtitleAnimations.frame(preview,2200L).highlighted)
    }

    @Test fun legacySubtitleAndImpossibleDurationKeepEmptyTimes() {
        val old=cue(text="Bom dia")
        assertTrue(SubtitleWordTimingEditorPolicy.previewCue(old,3000L).wordTimings.isEmpty())
        val tooShort=cue(text="Um dois tres",start=0,end=90)
        assertFalse(SubtitleWordTimingEditorPolicy.editable(tooShort))
        assertTrue(SubtitleWordTimingEditorPolicy.distribute(tooShort).isEmpty())
    }

    @Test fun invalidOverlapsCannotBeSavedAndPreviewNeverCreatesNewTimings() {
        val c=cue(text="A B",start=1000,end=2000)
        val bad=listOf(SubtitleWordTiming(1000L,1600L),SubtitleWordTiming(1500L,2000L))
        assertFalse(SubtitleWordTimingEditorPolicy.valid(c,bad))
        assertNull(SubtitleWordTimingEditorPolicy.moveBoundary(c,bad,0,1550L))
        val preview=SubtitleWordTimingEditorPolicy.previewCue(c.copy(wordTimings=bad),3000L)
        assertEquals(2,preview.wordTimings.size) // Prévia tolera horários ASR sobrepostos.
        assertEquals(bad,c.copy(wordTimings=bad).wordTimings) // Operação pura; original intacto.
    }
}
