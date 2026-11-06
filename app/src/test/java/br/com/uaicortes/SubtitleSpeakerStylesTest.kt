package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleSpeakerStylesTest {
    private fun fixture(): VideoProject = VideoProject(
        subtitleSpeakers=listOf(SubtitleSpeaker("cliente","Cliente",0xFF237FAD.toInt()),
            SubtitleSpeaker("agiota","Agiota",0xFF9963B2.toInt())),
        subtitles=listOf(
            SubtitleCue(id="a",text="Olá",startMs=100,endMs=850,speakerId="cliente"),
            SubtitleCue(id="b",text="Sim",startMs=900,endMs=1400,speakerId="agiota"),
            SubtitleCue(id="c",text="Como?",startMs=1500,endMs=2000,speakerId="cliente")
        ))
    private val appearance=SubtitleStyles.CopyOptions(appearance=true,position=false,animation=false)

    @Test fun appliedToOneSpeakerPreservesOtherSpeakerAndTimings() {
        val original=fixture()
        val source=original.subtitles[0].copy(color=0xFF123456.toInt(),sizeSp=30)
        val result=SubtitleSpeakerStyles.apply(original,"cliente",source,appearance)!!
        assertEquals(source.color,result.subtitles[0].color)
        assertEquals(source.color,result.subtitles[2].color)
        assertEquals(original.subtitles[1],result.subtitles[1])
        assertEquals(original.subtitles.map { it.startMs },result.subtitles.map { it.startMs })
        assertEquals(original.subtitles.map { it.text },result.subtitles.map { it.text })
        assertNotNull(result.subtitleSpeakerStyles["cliente"])
    }

    @Test fun manualOverrideSurvivesSubsequentGroupChange() {
        val base=SubtitleSpeakerStyles.apply(fixture(),"cliente",
            fixture().subtitles[0].copy(color=0xFF123456.toInt()),appearance)!!
        val original=base.subtitles[0]
        val customized=SubtitleSpeakerStyles.manualEdit(base,original,original.copy(color=0xFFFF0000.toInt()))
        assertTrue("a" in customized.subtitleStyleOverrides)
        val changed=SubtitleSpeakerStyles.apply(customized,"cliente",
            original.copy(color=0xFF00FF00.toInt()),appearance)!!
        assertEquals(0xFFFF0000.toInt(),changed.subtitles[0].color)
        assertEquals(0xFF00FF00.toInt(),changed.subtitles[2].color)
        assertEquals("Sim",changed.subtitles[1].text)
    }

    @Test fun timeOrTextEditDoesNotCreateStyleOverride() {
        val p=SubtitleSpeakerStyles.apply(fixture(),"cliente",
            fixture().subtitles[0].copy(color=0xFF123456.toInt()),appearance)!!
        val original=p.subtitles[0]
        val changed=SubtitleSpeakerStyles.manualEdit(p,original,
            original.copy(text="Novo texto",endMs=820,wordTimings=emptyList()))
        assertTrue(changed.subtitleStyleOverrides.isEmpty())
        assertEquals("Novo texto",changed.subtitles[0].text)
    }

    @Test fun resetRestoresOnlySelectedCue() {
        val p=SubtitleSpeakerStyles.apply(fixture(),"cliente",
            fixture().subtitles[0].copy(color=0xFF123456.toInt()),appearance)!!
        val changed=SubtitleSpeakerStyles.manualEdit(p,p.subtitles[0],
            p.subtitles[0].copy(color=0xFFFF0000.toInt()))
        val reset=SubtitleSpeakerStyles.reset(changed,"a")!!
        assertFalse("a" in reset.subtitleStyleOverrides)
        assertEquals(0xFF123456.toInt(),reset.subtitles[0].color)
        assertEquals(changed.subtitles[1],reset.subtitles[1])
    }

    @Test fun laterSpeakerAssignmentInheritsStyle() {
        val original=fixture()
        val configured=SubtitleSpeakerStyles.apply(original,"cliente",
            original.subtitles[0].copy(color=0xFF123456.toInt()),appearance)!!
        val result=SubtitleSpeakerPolicy.assign(configured,"b","cliente")!!
        assertEquals("cliente",result.subtitles[1].speakerId)
        assertEquals(0xFF123456.toInt(),result.subtitles[1].color)
        assertEquals(original.subtitles[1].startMs,result.subtitles[1].startMs)
    }

    @Test fun jsonKeepsStyleAndOverridesWithoutCopyingSpeechText() {
        val base=fixture()
        val style=SubtitleSpeakerStyles.apply(base,"cliente",
            base.subtitles[0].copy(color=0xFF123456.toInt()),appearance)!!
        val withOverride=SubtitleSpeakerStyles.manualEdit(style,style.subtitles[0],
            style.subtitles[0].copy(color=0xFFFF0000.toInt()))
        val serialized=withOverride.json()
        assertFalse(serialized.getJSONObject("subtitleSpeakerStyles").getJSONObject("cliente")
            .getString("text").contains("Olá"))
        val restored=VideoProject.read(serialized)
        assertEquals(style.subtitleSpeakerStyles["cliente"]?.color,
            restored.subtitleSpeakerStyles["cliente"]?.color)
        assertTrue("a" in restored.subtitleStyleOverrides)
        assertEquals(4,restored.json().getInt("schemaVersion"))
    }

    @Test fun legacyJsonAndRemovingSpeakerRemainCompatible() {
        val legacy=fixture().json().apply {
            remove("subtitleSpeakerStyles")
            remove("subtitleStyleOverrides")
        }
        val restored=VideoProject.read(legacy)
        assertTrue(restored.subtitleSpeakerStyles.isEmpty())
        assertTrue(restored.subtitleStyleOverrides.isEmpty())
        val styled=SubtitleSpeakerStyles.apply(restored,"cliente",
            restored.subtitles[0],appearance)!!
        val removed=SubtitleSpeakerPolicy.remove(styled,"cliente")
        assertTrue(removed.subtitleSpeakerStyles.isEmpty())
        assertEquals(null,removed.subtitles[0].speakerId)
    }
}
