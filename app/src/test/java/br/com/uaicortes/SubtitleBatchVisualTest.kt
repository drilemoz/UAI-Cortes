package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleBatchVisualTest {
    private val first=SubtitleCue(id="a", text="Primeira",startMs=0,endMs=900, x=.25f,y=.6f)
    private val second=SubtitleCue(id="b",text="Segunda",startMs=950,endMs=2200,x=.8f,y=.8f)
    @Test fun appearanceAndAnimationDoNotChangeTextTimingOrPosition() {
        val source=first.copy(sizeSp=40,color=0xFF112233.toInt(),animation="fade",animationSpeed=1.8f)
        val result=SubtitleStyles.applySelectedToAll(listOf(first,second),source,
            SubtitleStyles.CopyOptions(appearance=true,position=false,animation=true))
        assertEquals(listOf("a","b"),result.map{it.id})
        assertEquals("Segunda",result[1].text)
        assertEquals(second.startMs,result[1].startMs)
        assertEquals(second.endMs,result[1].endMs)
        assertEquals(second.x,result[1].x,0f)
        assertEquals(second.y,result[1].y,0f)
        assertEquals(40,result[1].sizeSp)
        assertEquals(source.animation,result[1].animation)
        assertEquals(1.8f,result[1].animationSpeed,0f)
    }
    @Test fun positionRequiresExplicitSelection() {
        val source=first.copy(x=.4f,y=.3f)
        val changed=SubtitleStyles.applySelectedToAll(listOf(first,second),source,
            SubtitleStyles.CopyOptions(appearance=false,position=true,animation=false))
        assertEquals(.4f,changed[1].x,0f)
        assertEquals(.3f,changed[1].y,0f)
        assertEquals(second.text,changed[1].text)
        assertEquals(second.startMs,changed[1].startMs)
    }
    @Test fun undoRejectsChangesMadeAfterTheBatch() {
        val after=SubtitleStyles.applySelectedToAll(listOf(first,second),first.copy(sizeSp=31),
            SubtitleStyles.CopyOptions(appearance=true,position=false,animation=false))
        val undo=SubtitleBatchUndo(listOf(first,second),after)
        assertEquals(listOf(first,second),undo.restoreIfCurrent(after))
        assertNull(undo.restoreIfCurrent(after.mapIndexed { i, cue ->
            if (i==1) cue.copy(text="Texto editado depois") else cue
        }))
    }

}
