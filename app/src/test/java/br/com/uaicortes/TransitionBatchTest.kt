package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class TransitionBatchTest {
    private fun project():VideoProject {
        val clips=(1..4).map { Clip(uri="clip$it",title="Clip $it",sourceDurationMs=4000) }
        return VideoProject(clips=clips,transitions=listOf(
            ClipTransition(clips[0].id,clips[1].id,TransitionEffects.BLACK,500),
            ClipTransition(clips[1].id,clips[2].id,TransitionEffects.WHITE,500)
        ))
    }
    @Test fun applyOnePreservesOtherJunctions() {
        val source=project()
        val modified=TransitionBatch.apply(source,1,TransitionEffects.SEPIA,750,false)
        assertEquals(2,modified.transitions.size)
        assertEquals(TransitionEffects.BLACK,modified.transitions[0].kind)
        assertEquals(TransitionEffects.SEPIA,EditFeatures.boundary(modified,1)?.kind)
        assertEquals(500L,EditFeatures.boundary(source,1)?.durationMs)
        assertEquals(source.clips,modified.clips)
    }
    @Test fun applyAllCreatesOneTransitionPerJoin() {
        val source=project()
        val changed=TransitionBatch.apply(source,0,TransitionEffects.BLUE,700,true)
        assertEquals(3,changed.transitions.size)
        assertTrue(changed.transitions.all { it.kind==TransitionEffects.BLUE && it.durationMs==700L })
        assertEquals(source.clips,changed.clips)
    }
    @Test fun cutRemovesOnlySelectedOrAll() {
        val source=project()
        val single=TransitionBatch.apply(source,0,TransitionEffects.CUT,500,false)
        assertEquals(1,single.transitions.size)
        val all=TransitionBatch.apply(source,0,TransitionEffects.CUT,500,true)
        assertTrue(all.transitions.isEmpty())
    }
    @Test fun invalidInputCannotModifyProject() {
        val source=project()
        assertEquals(source,TransitionBatch.apply(source,-1,TransitionEffects.WHITE,500,false))
        assertEquals(source,TransitionBatch.apply(source,0,"futuro desconhecido",500,true))
        val empty = VideoProject()
        assertEquals(empty,TransitionBatch.apply(empty,0,TransitionEffects.BLUE,500,true))
    }
}
