package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleAnimationsTest {
    private val cue = SubtitleCue(id="a", text="Olá! Bem vindo.",startMs=1000L,endMs=5000L)

    @Test fun defaultAndLegacyAnimationAreStable() {
        assertEquals(12,cue.sizeSp)
        assertEquals(SubtitleAnimations.NONE,cue.animation)
        val legacy=cue.json().apply { remove("sizeSp");remove("animation");remove("animationSpeed") }
        val loaded=SubtitleCue.read(legacy)
        assertEquals(22,loaded.sizeSp)
        assertEquals(SubtitleAnimations.NONE,loaded.animation)
        assertEquals(1f,loaded.animationSpeed,0.001f)
    }

    @Test fun animationsRoundTrip() {
        for ((kind,_) in SubtitleAnimations.choices) {
            val item=cue.copy(animation=kind,animationSpeed=1.5f)
            val back=SubtitleCue.read(item.json())
            assertEquals(kind,back.animation)
            assertEquals(1.5f,back.animationSpeed,.001f)
        }
        assertEquals(SubtitleAnimations.NONE,SubtitleAnimations.normalize("code"))
        assertEquals(1f,SubtitleAnimations.speed(Float.NaN),.001f)
    }

    @Test fun typewriterAndPhrasesUseActualTime() {
        val typed=cue.copy(animation=SubtitleAnimations.TYPEWRITER)
        assertEquals("",SubtitleAnimations.frame(typed,1000L).text)
        assertEquals(cue.text,SubtitleAnimations.frame(typed,5000L).text)
        val phrased=cue.copy(animation=SubtitleAnimations.PHRASES)
        assertEquals("Olá!",SubtitleAnimations.frame(phrased,1000L).text)
        assertEquals("Bem vindo.",SubtitleAnimations.frame(phrased,4500L).text)
    }

    @Test fun highlightedWordAlwaysWithinText() {
        val karaoke=cue.copy(animation=SubtitleAnimations.KARAOKE)
        for (ms in 1000L..5000L step 113) {
            val frame=SubtitleAnimations.frame(karaoke,ms)
            assertNotNull(frame.highlighted)
            val highlight=frame.highlighted!!
            assertTrue(highlight.first>=0 && highlight.last<frame.text.length)
        }
    }

    @Test fun effectsHaveBoundedAlphaScaleAndOffset() {
        for ((kind,_) in SubtitleAnimations.choices) {
            val item=cue.copy(animation=kind)
            for (ms in 1000L..5000L step 101) {
                val f=SubtitleAnimations.frame(item,ms)
                assertTrue(f.alpha in 0f..1f)
                assertTrue(f.scale in .60f..1.31f)
                assertTrue(f.slideY in -.2f.. .2f)
                assertTrue(f.slideX in -.2f.. .2f)
            }
        }
    }

    @Test fun presetsAlwaysHaveRealAnimationsAndPreserveTiming() {
        val cue=SubtitleCue(text="Exemplo",startMs=150,endMs=900,animation=SubtitleAnimations.NONE)
        for(preset in SubtitleStyles.presets) {
            assertTrue(SubtitleAnimations.choices.any{ it.first == preset.animation })
            val updated=SubtitleStyles.applyToCue(cue,preset)
            assertEquals(preset.animation,updated.animation)
            assertEquals(cue.text,updated.text)
            assertEquals(cue.startMs,updated.startMs)
            assertEquals(cue.endMs,updated.endMs)
        }
    }

    @Test fun copyOptionsDoNotRewriteContent() {
        val other=cue.copy(id="b",text="Texto diferente",startMs=7000,endMs=8500,
            x=.12f,y=.22f)
        val source=cue.copy(color=0xFF00FFFF.toInt(),sizeSp=30,
            animation=SubtitleAnimations.FADE,animationSpeed=1.5f,x=.6f,y=.5f)
        val result=SubtitleStyles.applySelectedToAll(listOf(source,other),source,
            SubtitleStyles.CopyOptions(appearance=false,position=false,animation=true))
        assertEquals(other.text,result[1].text)
        assertEquals(other.startMs,result[1].startMs)
        assertEquals(other.endMs,result[1].endMs)
        assertEquals(other.id,result[1].id)
        assertEquals(other.x,result[1].x,.001f)
        assertEquals(other.sizeSp,result[1].sizeSp)
        assertEquals(SubtitleAnimations.FADE,result[1].animation)
    }

    @Test fun newEffectsPreserveTextAndHaveFiniteMotion() {
        assertEquals(27,SubtitleAnimations.choices.size)
        val known=SubtitleAnimations.groups.flatMap { it.second }.toSet()
        assertEquals(known.size,SubtitleAnimations.choices.size)
        assertTrue(known.contains(SubtitleAnimations.WORD_REVEAL))
        for(id in known) {
            val test=cue.copy(animation=id)
            for(ms in 1000L..5000L step 53L) {
                val f=SubtitleAnimations.frame(test,ms)
                assertTrue(f.alpha.isFinite())
                assertTrue(f.scale.isFinite())
                assertTrue(f.slideX.isFinite())
                assertTrue(f.slideY.isFinite())
            }
        }
        val words=cue.copy(animation=SubtitleAnimations.WORD_REVEAL)
        assertEquals("",SubtitleAnimations.frame(words,1000L).text)
        assertEquals(cue.text,SubtitleAnimations.frame(words,5000L).text)
    }
}
