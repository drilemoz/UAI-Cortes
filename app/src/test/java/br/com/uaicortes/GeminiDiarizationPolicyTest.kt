package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class GeminiDiarizationPolicyTest {
    private val session1 = "0123456789abcdef0123456789abcdef"
    private val session2 = "fedcba9876543210fedcba9876543210"
    private fun suggested(session: String, speaker: String) =
        GeminiDiarizationPolicy.provisionalId(session,speaker)!!
    private fun cue(text: String, id: String?) = SubtitleCue(
        text=text,startMs=100,endMs=700,color=-1,speakerId=id)

    @Test fun provisionalIdsAreScopedToAudioAndNeverGuessPersonName() {
        val first=suggested(session1,"spk_1")
        val second=suggested(session2,"spk_1")
        assertNotEquals(first,second)
        assertEquals("Voz 1",GeminiDiarizationPolicy.label(first))
        assertEquals("Voz 1",GeminiDiarizationPolicy.label(second))
        assertNull(GeminiDiarizationPolicy.provisionalId(session1,"Maria"))
        assertNull(GeminiDiarizationPolicy.provisionalId(session1,"spk_9"))
    }

    @Test fun createsOnlyAfterExplicitPreparationAndKeepsTextAndColors() {
        val key=suggested(session1,"spk_1")
        val base=VideoProject(subtitles=listOf(cue("Antiga",null)))
        val generated=listOf(cue("Fala nova",key))
        val prepared=GeminiDiarizationPolicy.prepare(base,generated,false,
            emptyMap(),mapOf(key to "João"))!!
        assertEquals(0,base.subtitleSpeakers.size)
        assertEquals(1,prepared.subtitleSpeakers.size)
        assertEquals("João",prepared.subtitleSpeakers.single().name)
        assertEquals(key,prepared.subtitles.last().speakerId)
        assertEquals(listOf("Antiga","Fala nova"),prepared.subtitles.map { it.text })
        assertEquals(-1,prepared.subtitles.last().color)
    }

    @Test fun canLinkExistingOrLeaveUnidentified() {
        val a=suggested(session1,"spk_1")
        val b=suggested(session1,"spk_2")
        val existing=SubtitleSpeaker("stored","Personagem A",123)
        val project=VideoProject(subtitleSpeakers=listOf(existing))
        val result=GeminiDiarizationPolicy.prepare(project,
            listOf(cue("A",a),cue("B",b)),false,
            mapOf(a to "stored",b to GeminiDiarizationPolicy.IGNORE),emptyMap())!!
        assertEquals(1,result.subtitleSpeakers.size)
        assertEquals(listOf("stored",null),result.subtitles.map { it.speakerId })
    }

    @Test fun rejectsDuplicateNameOrMissingTargetWithoutPartialEdits() {
        val a=suggested(session1,"spk_1")
        val existing=SubtitleSpeaker("x","Voz 1",123)
        val project=VideoProject(subtitleSpeakers=listOf(existing))
        assertNull(GeminiDiarizationPolicy.prepare(project,listOf(cue("Nova",a)),false,
            emptyMap(),emptyMap()))
        assertNull(GeminiDiarizationPolicy.prepare(project,listOf(cue("Nova",a)),false,
            mapOf(a to "missing"),emptyMap()))
        assertEquals(1,project.subtitleSpeakers.size)
    }

    @Test fun preservesLegacySubtitlesWhenNotReplacing() {
        val key=suggested(session1,"spk_2")
        val base=VideoProject(subtitles=listOf(cue("Antigo",null)))
        val result=GeminiDiarizationPolicy.prepare(base,listOf(cue("Novo",key)),false,
            mapOf(key to GeminiDiarizationPolicy.IGNORE),emptyMap())!!
        assertEquals(2,result.subtitles.size)
        assertTrue(result.subtitles.all { it.speakerId == null })
    }

    @Test fun refusesMoreThanSixteenCharactersOnlyInDisplayNameLimitAndMaxSpeakers() {
        val key=suggested(session1,"spk_1")
        val full=VideoProject(subtitleSpeakers=(1..16).map { i ->
            SubtitleSpeaker("p$i","Personagem $i",i)
        })
        assertNull(GeminiDiarizationPolicy.prepare(full,listOf(cue("Fala",key)),false,
            emptyMap(),emptyMap()))
        assertNotNull(GeminiDiarizationPolicy.prepare(full,listOf(cue("Fala",key)),false,
            mapOf(key to GeminiDiarizationPolicy.IGNORE),emptyMap()))
        assertNull(GeminiDiarizationPolicy.prepare(VideoProject(),listOf(cue("Fala",key)),false,
            emptyMap(),mapOf(key to "x".repeat(41))))
    }
}
