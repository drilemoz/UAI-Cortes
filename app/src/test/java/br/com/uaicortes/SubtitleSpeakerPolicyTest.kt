package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleSpeakerPolicyTest {
    private fun project() = VideoProject(subtitles = listOf(
        SubtitleCue(id = "a", text = "Olá", startMs = 100, endMs = 900, color = -1),
        SubtitleCue(id = "b", text = "Sim", startMs = 900, endMs = 1500, color = -1)))

    @Test fun legacyProjectRemainsUnassigned() {
        val legacy = project().json().apply { remove("subtitleSpeakers") }
        val arr = legacy.getJSONArray("subtitles")
        (0 until arr.length()).forEach { i -> arr.getJSONObject(i).remove("speakerId") }
        val restored = VideoProject.read(legacy)
        assertTrue(restored.subtitleSpeakers.isEmpty())
        assertTrue(restored.subtitles.all { it.speakerId == null })
        assertEquals(4, restored.json().getInt("schemaVersion"))
    }

    @Test fun assignAndJsonRoundTripPreserveTimings() {
        val base = project()
        val named = SubtitleSpeakerPolicy.add(base, "Carlos", "speaker1")!!
        val assigned = SubtitleSpeakerPolicy.assign(named, "a", "speaker1")!!
        val restored = VideoProject.read(assigned.json())
        assertEquals("Carlos", restored.subtitleSpeakers.single().name)
        assertEquals("speaker1", restored.subtitles.first().speakerId)
        assertEquals(100L, restored.subtitles.first().startMs)
        assertEquals(900L, restored.subtitles.first().endMs)
        assertEquals(-1, restored.subtitles.first().color)
    }

    @Test fun rejectInvalidSpeakerAndCue() {
        val initial = project()
        assertNull(SubtitleSpeakerPolicy.assign(initial, "a", "absent"))
        assertNull(SubtitleSpeakerPolicy.assign(initial, "absent", null))
        assertNull(SubtitleSpeakerPolicy.add(initial, "", "a"))
    }

    @Test fun namesCannotDuplicateIgnoringCase() {
        val original = SubtitleSpeakerPolicy.add(project(), "Maria", "one")!!
        assertNull(SubtitleSpeakerPolicy.add(original, "maria", "two"))
        assertNull(SubtitleSpeakerPolicy.rename(original, "other", "Maria", -1))
        assertEquals("Ana", SubtitleSpeakerPolicy.rename(original, "one", "Ana", -1)!!
            .subtitleSpeakers.single().name)
    }

    @Test fun removalUnassignsWithoutDeletingCues() {
        val assigned = SubtitleSpeakerPolicy.assign(
            SubtitleSpeakerPolicy.add(project(), "Pessoa", "p")!!, "b", "p")!!
        val removed = SubtitleSpeakerPolicy.remove(assigned, "p")
        assertEquals(2, removed.subtitles.size)
        assertEquals(listOf(100L, 900L), removed.subtitles.map { it.startMs })
        assertTrue(removed.subtitles.all { it.speakerId == null })
    }

    @Test fun colorsOnlyChangeAfterExplicitAction() {
        val assigned = SubtitleSpeakerPolicy.assign(
            SubtitleSpeakerPolicy.add(project(), "Pessoa", "p")!!, "b", "p")!!
        assertEquals(-1, assigned.subtitles.last().color)
        val tinted = SubtitleSpeakerPolicy.applyColors(assigned)
        assertEquals(assigned.subtitleSpeakers.single().color, tinted.subtitles.last().color)
        assertEquals(-1, tinted.subtitles.first().color)
        assertEquals(assigned.subtitles.map { it.text }, tinted.subtitles.map { it.text })
        assertEquals(assigned.subtitles.map { it.startMs }, tinted.subtitles.map { it.startMs })
    }

    @Test fun filterDoesNotModifyProject() {
        val assigned = SubtitleSpeakerPolicy.assign(
            SubtitleSpeakerPolicy.add(project(), "Pessoa", "p")!!, "b", "p")!!
        assertEquals(listOf("b"), SubtitleSpeakerPolicy.filter(assigned.subtitles,
            "p", assigned.subtitleSpeakers).map { it.id })
        assertEquals(listOf("a"), SubtitleSpeakerPolicy.filter(assigned.subtitles,
            "__unassigned__", assigned.subtitleSpeakers).map { it.id })
        assertEquals(2, assigned.subtitles.size)
    }
}
