package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitlePreviewPolicyTest {
    private val first = SubtitleCue(id="a",text="Primeira",startMs=1_000,endMs=2_000)
    private val second = SubtitleCue(id="b",text="Segunda",startMs=2_000,endMs=3_000)
    private val late = SubtitleCue(id="c",text="Depois da pausa",startMs=5_000,endMs=7_000)

    @Test fun continuesToNextCueExactlyAtTheBoundary() {
        val cues=listOf(first,second,late)
        assertEquals(listOf("a"),SubtitlePreviewPolicy.activeAt(cues,1_999).map { it.id })
        assertEquals(listOf("b"),SubtitlePreviewPolicy.activeAt(cues,2_000).map { it.id })
        assertEquals(listOf("b"),SubtitlePreviewPolicy.activeAt(cues,2_999).map { it.id })
    }

    @Test fun gapsAndEndOfProjectHaveNoPhantomSubtitle() {
        val cues=listOf(first,second,late)
        assertTrue(SubtitlePreviewPolicy.activeAt(cues,0).isEmpty())
        assertTrue(SubtitlePreviewPolicy.activeAt(cues,3_000).isEmpty())
        assertEquals(listOf(late),SubtitlePreviewPolicy.activeAt(cues,6_000))
        assertTrue(SubtitlePreviewPolicy.activeAt(cues,7_000).isEmpty())
    }

    @Test fun overlappingIntervalsAreBothVisibleInOriginalOrder() {
        val overlapping=SubtitleCue(id="d",text="Sobreposta",startMs=1_500,endMs=2_500)
        assertEquals(listOf(first,overlapping),
            SubtitlePreviewPolicy.activeAt(listOf(first,overlapping,second),1_700))
    }

    @Test fun invalidEmptyCuesAreNotDisplayed() {
        val invalid=SubtitleCue(id="e",text="Inválida",startMs=3_000,endMs=3_000)
        val blank=SubtitleCue(id="f",text="   ",startMs=0,endMs=10_000)
        assertTrue(SubtitlePreviewPolicy.activeAt(listOf(invalid,blank),3_000).isEmpty())
    }
}
