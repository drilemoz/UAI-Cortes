package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class VoiceIaSpeakerVoicePolicyTest {
    private fun sample(): VideoProject {
        val a = SubtitleCue(id = "a", text = "Você que é o agiota", startMs = 0, endMs = 1500,
            speakerId = "cliente")
        val b = SubtitleCue(id = "b", text = "Sou eu mesmo", startMs = 1600, endMs = 3000,
            speakerId = "agiota")
        val c = SubtitleCue(id = "c", text = "Outra fala", startMs = 3200, endMs = 4400,
            speakerId = "cliente")
        return VideoProject(subtitles = listOf(a, b, c), subtitleSpeakers = listOf(
            SubtitleSpeaker("cliente", "Cliente", 0xFF9963B2.toInt()),
            SubtitleSpeaker("agiota", "Agiota", 0xFF237FAD.toInt())))
    }

    @Test fun eachSpeakerInheritsItsVoiceAcrossSeparateSubtitles() {
        val base = sample()
        val assigned = VoiceIaSpeakerVoicePolicy.speakerVoice(base, "cliente", "voz-azul")!!
        val both = VoiceIaSpeakerVoicePolicy.speakerVoice(assigned, "agiota", "voz-vermelha")!!
        assertEquals("voz-azul", VoiceIaSpeakerVoicePolicy.preferred(both.subtitles[0],
            both.subtitleSpeakers, both.voiceIaVoices))
        assertEquals("voz-vermelha", VoiceIaSpeakerVoicePolicy.preferred(both.subtitles[1],
            both.subtitleSpeakers, both.voiceIaVoices))
        assertEquals("voz-azul", VoiceIaSpeakerVoicePolicy.preferred(both.subtitles[2],
            both.subtitleSpeakers, both.voiceIaVoices))
        assertEquals(sample().subtitles, both.subtitles)
    }

    @Test fun cueOverridesSpeakerWithoutChangingOtherCues() {
        val character = VoiceIaSpeakerVoicePolicy.speakerVoice(sample(), "cliente", "voz1")!!
        val changed = VoiceIaSpeakerVoicePolicy.cueVoice(character, "a", "voz-especial")!!
        assertEquals("voz-especial", VoiceIaSpeakerVoicePolicy.preferred(changed.subtitles[0],
            changed.subtitleSpeakers, changed.voiceIaVoices))
        assertEquals("voz1", VoiceIaSpeakerVoicePolicy.preferred(changed.subtitles[2],
            changed.subtitleSpeakers, changed.voiceIaVoices))
        val inherited = VoiceIaSpeakerVoicePolicy.cueVoice(changed, "a", null)!!
        assertEquals("voz1", VoiceIaSpeakerVoicePolicy.preferred(inherited.subtitles[0],
            inherited.subtitleSpeakers, inherited.voiceIaVoices))
    }

    @Test fun voicePreferencesSurviveJsonAndLegacyRemainCompatible() {
        val linked = VoiceIaSpeakerVoicePolicy.cueVoice(
            VoiceIaSpeakerVoicePolicy.speakerVoice(sample(), "agiota", "GEMINI_A")!!, "b", "GEMINI_B")!!
        val restored = VideoProject.read(linked.json())
        assertEquals(linked.voiceIaVoices, restored.voiceIaVoices)
        assertEquals(linked.subtitles, restored.subtitles)
        assertEquals(4, restored.json().getInt("schemaVersion"))
        val legacy = linked.json().apply { remove("voiceIaVoices") }
        assertEquals(VoiceIaVoiceAssignments(), VideoProject.read(legacy).voiceIaVoices)
    }

    @Test fun generalVoiceOnlyAppliesAfterConfirmationAndClearsOverrides() {
        val base = VoiceIaSpeakerVoicePolicy.cueVoice(
            VoiceIaSpeakerVoicePolicy.speakerVoice(sample(), "cliente", "custom")!!, "a", "solo")!!
        val general = VoiceIaSpeakerVoicePolicy.all(base, "general")!!
        assertTrue(general.voiceIaVoices.cueVoices.isEmpty())
        assertTrue(general.voiceIaVoices.speakerVoices.isEmpty())
        assertEquals("general", general.voiceIaVoices.defaultVoiceId)
        assertEquals("general", VoiceIaSpeakerVoicePolicy.preferred(general.subtitles[0],
            general.subtitleSpeakers, general.voiceIaVoices))
        assertEquals(base.subtitles, general.subtitles)
    }

    @Test fun removingCharacterDoesNotDeleteSubtitlesOrOrphanPreferences() {
        val linked = VoiceIaSpeakerVoicePolicy.speakerVoice(sample(), "cliente", "voz1")!!
        val removed = SubtitleSpeakerPolicy.remove(linked, "cliente")
        assertEquals(3, removed.subtitles.size)
        assertNull(removed.subtitles[0].speakerId)
        assertFalse("cliente" in removed.voiceIaVoices.speakerVoices)
        assertNull(VoiceIaSpeakerVoicePolicy.preferred(removed.subtitles[0],
            removed.subtitleSpeakers, removed.voiceIaVoices))
    }

    @Test fun invalidIdsAndMissingCharactersCannotBeSaved() {
        assertNull(VoiceIaSpeakerVoicePolicy.speakerVoice(sample(), "other", "voz"))
        assertNull(VoiceIaSpeakerVoicePolicy.cueVoice(sample(), "other", "voz"))
        assertNull(VoiceIaSpeakerVoicePolicy.all(sample(), ""))
        assertNull(VoiceIaSpeakerVoicePolicy.speakerVoice(sample(), "cliente", "\n"))
    }

    @Test fun voiceChangesStaleOldQueueWithoutRetryingNetwork() {
        val queue = VoiceIaQueuePolicy.create("project", listOf(
            VoiceIaQueueItem("a", "a".repeat(64), "oldVoice", "gemini", "Neutra", "Siga o texto"),
            VoiceIaQueueItem("b", "b".repeat(64), "goodVoice", "gemini", "Neutra", "Siga o texto")))
        val reconciled = VoiceIaQueuePolicy.reconcileSelections(queue,
            mapOf("a" to "newVoice", "b" to "goodVoice"))
        assertEquals(VoiceIaQueueStage.STALE, reconciled.items[0].stage)
        assertEquals(VoiceIaQueueStage.PENDING, reconciled.items[1].stage)
        assertEquals(1, reconciled.pending)
    }

    @Test fun classicalGeneratedStyleMatchesSrtButKeepsWordTimingsAndSpeaker() {
        val imported = SubtitleCue(id = "srt", text = "Boa noite", startMs = 500, endMs = 1000)
        val generated = imported.copy(sizeSp = 20, outlineWidth = 3,
            backgroundColor = SubtitleStyles.NO_BACKGROUND, shadowOffsetDp = 2,
            animation = SubtitleAnimations.KARAOKE,
            wordTimings = listOf(SubtitleWordTiming(500, 750), SubtitleWordTiming(750, 1000)),
            speakerId = "cliente")
        val converted = SubtitleAppearancePolicy.generated(generated,
            SubtitleAppearanceMode.CLASSIC_SRT, 12)
        assertEquals(imported.sizeSp, converted.sizeSp)
        assertEquals(imported.backgroundColor, converted.backgroundColor)
        assertEquals(imported.outlineWidth, converted.outlineWidth)
        assertEquals(imported.shadowOffsetDp, converted.shadowOffsetDp)
        assertEquals(imported.animation, converted.animation)
        assertEquals(generated.wordTimings, converted.wordTimings)
        assertEquals("cliente", converted.speakerId)
        assertEquals(generated.text, converted.text)
        assertEquals(generated.startMs, converted.startMs)
    }
}
