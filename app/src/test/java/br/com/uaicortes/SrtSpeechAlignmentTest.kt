package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SrtSpeechAlignmentTest {
    private fun caption(text: String, start: Long, end: Long) = SubtitleCue(
        text = text, startMs = start, endMs = end)

    private fun speech(text: String, vararg timing: Pair<Long, Long>) = SubtitleCue(
        text = text, startMs = timing.first().first, endMs = timing.last().second,
        wordTimings = timing.map { SubtitleWordTiming(it.first, it.second) })

    @Test fun importedTextAndStyleAreRetainedWithExactWordTimesFromOffsetTrack() {
        val srt = caption("Olá, MEU amigo!", 1_000, 3_000)
        val source = speech("ola meu amigo", 200L to 410L, 450L to 700L, 730L to 890L)
        val result = SrtSpeechAlignment.align(listOf(srt), listOf(source), 7_000, 1_400)
        val cue = result.cues.single()
        assertEquals(1, result.adjusted)
        assertEquals(srt.id, cue.id)
        assertEquals(srt.text, cue.text)
        assertEquals(srt.sizeSp, cue.sizeSp)
        assertEquals(1_600L, cue.startMs)
        assertEquals(2_290L, cue.endMs)
        assertEquals(listOf(SubtitleWordTiming(1600, 1810), SubtitleWordTiming(1850, 2100),
            SubtitleWordTiming(2130, 2290)), cue.wordTimings)
    }

    @Test fun repeatedAndUnmatchedPhrasesAreNotGuessed() {
        val srt = listOf(caption("Bom dia", 2_000, 3_200), caption("Não invente", 5_000, 5_700))
        val spoken = listOf(
            speech("bom dia", 1750L to 1900L, 1940L to 2050L),
            speech("bom dia", 2200L to 2310L, 2350L to 2500L))
        val result = SrtSpeechAlignment.align(srt, spoken, 6_000)
        assertEquals(0, result.matched)
        assertEquals(srt, result.cues)
    }

    @Test fun coherentRetimingOfAdjacentCaptionsIsAllowed() {
        val imported = listOf(caption("Tudo bem", 1_000, 2_000), caption("Boa tarde", 2_000, 3_000))
        val transcript = listOf(speech("tudo bem", 1500L to 1800L, 1830L to 2390L),
            speech("boa tarde", 2500L to 2790L, 2830L to 3490L))
        val result = SrtSpeechAlignment.align(imported, transcript, 5_000)
        assertEquals(2, result.adjusted)
        assertEquals(listOf(1500L, 2500L), result.cues.map { it.startMs })
    }

    @Test fun newOverlapWithUnmatchedTextIsNotIntroduced() {
        val imported = listOf(caption("Tudo bem", 1_000, 2_000), caption("Sem fonte", 2_000, 3_000))
        val transcript = listOf(speech("tudo bem", 1500L to 1800L, 1830L to 2390L))
        assertEquals(imported, SrtSpeechAlignment.align(imported, transcript, 5_000).cues)
    }

    @Test fun newCaptionsUse14WhileSavedSizesAndOverridesRemainAdjustable() {
        val parsed = EditFeatures.parseSrt("1\n00:00:01,000 --> 00:00:02,000\nOlá\n").single()
        assertEquals(14, parsed.sizeSp)
        assertEquals(14, SubtitleAppearancePolicy.defaultSize())
        val newPink = SubtitleAppearancePolicy.generated(parsed,
            SubtitleAppearanceMode.KARAOKE_ROSA, SubtitleAppearancePolicy.defaultSize())
        assertEquals(14, newPink.sizeSp)
        assertEquals(2, newPink.outlineWidth)
        assertEquals(0, newPink.shadowOffsetDp)
        assertEquals(24, SubtitleCue.read(parsed.copy(sizeSp = 24).json()).sizeSp)
    }
}
