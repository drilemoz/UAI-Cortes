package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class VoiceIaStylePolicyTest {
    private fun sample() = VideoProject(subtitles = listOf(
        SubtitleCue(id="a", text="Cliente fala", startMs=0, endMs=1500, speakerId="cliente"),
        SubtitleCue(id="b", text="Outra fala", startMs=1600, endMs=2500, speakerId="cliente"),
        SubtitleCue(id="c", text="Agiota fala", startMs=2600, endMs=3300, speakerId="agiota")),
        subtitleSpeakers = listOf(
            SubtitleSpeaker("cliente", "Cliente", 0xFF9963B2.toInt()),
            SubtitleSpeaker("agiota", "Agiota", 0xFF237FAD.toInt())))

    @Test fun styleBySpeakerInheritsAndCueWins() {
        val base = VoiceIaStylePolicy.speakerStyle(sample(), "cliente", "Assustada")!!
        assertEquals("Assustada", VoiceIaStylePolicy.preferred(base.subtitles[0],
            base.subtitleSpeakers, base.voiceIaVoices))
        assertEquals("Assustada", VoiceIaStylePolicy.preferred(base.subtitles[1],
            base.subtitleSpeakers, base.voiceIaVoices))
        val exceptional = VoiceIaStylePolicy.cueStyle(base, "a", "Sarcástica")!!
        assertEquals("Sarcástica", VoiceIaStylePolicy.preferred(exceptional.subtitles[0],
            exceptional.subtitleSpeakers, exceptional.voiceIaVoices))
        assertEquals("Assustada", VoiceIaStylePolicy.preferred(exceptional.subtitles[1],
            exceptional.subtitleSpeakers, exceptional.voiceIaVoices))
        val restored = VoiceIaStylePolicy.cueStyle(exceptional, "a", null)!!
        assertEquals("Assustada", VoiceIaStylePolicy.preferred(restored.subtitles[0],
            restored.subtitleSpeakers, restored.voiceIaVoices))
    }

    @Test fun generalStylePersistsAndDoesNotDeleteSpeakerVoices() {
        val withVoice = VoiceIaSpeakerVoicePolicy.speakerVoice(sample(), "agiota", "VozA")!!
        val assigned = VoiceIaStylePolicy.speakerStyle(withVoice, "agiota", "Humorística")!!
        val reset = VoiceIaStylePolicy.all(assigned, "Neutra")!!
        assertEquals("VozA", reset.voiceIaVoices.speakerVoices["agiota"])
        assertTrue(reset.voiceIaVoices.speakerStyles.isEmpty())
        assertEquals("Neutra", VoiceIaStylePolicy.preferred(reset.subtitles[2],
            reset.subtitleSpeakers, reset.voiceIaVoices))
        assertEquals(assigned.subtitles, reset.subtitles)
    }

    @Test fun voiceGlobalChangePreservesStyles() {
        val styled = VoiceIaStylePolicy.speakerStyle(sample(), "cliente", "Dramática")!!
        val changed = VoiceIaSpeakerVoicePolicy.all(styled, "NewVoice")!!
        assertEquals("Dramática", changed.voiceIaVoices.speakerStyles["cliente"])
    }

    @Test fun stylesSurviveJsonAndOlderProjectsStillWork() {
        val styled = VoiceIaStylePolicy.cueStyle(
            VoiceIaStylePolicy.speakerStyle(sample(), "cliente", "Assustada")!!,
            "b", "Sarcástica")!!
        val copied = VideoProject.read(styled.json())
        assertEquals(styled.voiceIaVoices, copied.voiceIaVoices)
        assertEquals(styled.subtitles, copied.subtitles)
        assertEquals(4, copied.json().getInt("schemaVersion"))
        val legacy = styled.json().apply { remove("voiceIaVoices") }
        assertEquals(VoiceIaVoiceAssignments(), VideoProject.read(legacy).voiceIaVoices)
    }

    @Test fun invalidStyleRejectedAndSpeakerRemovedCleansMap() {
        assertFalse(VoiceIaStylePolicy.valid("aleatória\n"))
        assertNull(VoiceIaStylePolicy.speakerStyle(sample(), "cliente", "inexistente"))
        assertNull(VoiceIaStylePolicy.speakerStyle(sample(), "inexistente", "Neutra"))
        assertNull(VoiceIaStylePolicy.cueStyle(sample(), "fora", "Neutra"))
        val styled = VoiceIaStylePolicy.speakerStyle(sample(), "cliente", "Assustada")!!
        val removed = SubtitleSpeakerPolicy.remove(styled, "cliente")
        assertFalse("cliente" in removed.voiceIaVoices.speakerStyles)
        assertNull(removed.subtitles[0].speakerId)
    }

    @Test fun modifiedStyleAndModelMustStaleRecoveredQueue() {
        val sample = VoiceIaQueuePolicy.create("project", listOf(
            VoiceIaQueueItem("a", "a".repeat(64), "Voz", "Lite", "old", "request"),
            VoiceIaQueueItem("b", "b".repeat(64), "Voz", "Lite", "new", "request")))
        val checked = VoiceIaQueuePolicy.reconcileSelections(sample,
            mapOf("a" to "Voz", "b" to "Voz"), mapOf("a" to "new", "b" to "new"), "Lite")
        assertEquals(VoiceIaQueueStage.STALE, checked.items[0].stage)
        assertEquals(VoiceIaQueueStage.PENDING, checked.items[1].stage)
        val switched = VoiceIaQueuePolicy.reconcileSelections(sample,
            mapOf("a" to "Voz", "b" to "Voz"), mapOf("a" to "old", "b" to "new"), "Flash")
        assertEquals(2, switched.stale)
    }
}
