package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleKaraokeBadge242Test {
    private val preset=SubtitleStudioPresets.recommended.first { it.name=="Karaokê rosa do vídeo" }

    @Test fun styleHasSameColorsAndNoWholeCaptionBackground() {
        assertEquals(SubtitleAnimations.KARAOKE_BADGE,preset.animation)
        assertEquals(0xFFE81050.toInt(),preset.highlightColor)
        assertEquals(0xFFFFFFFF.toInt(),preset.foreground)
        assertEquals(SubtitleStyles.NO_BACKGROUND,preset.background)
        assertEquals(4,preset.outlineWidth)
        assertEquals("upper",preset.textCase)
    }

    @Test fun activeWordFollowsRealTimingsAndDoesNotModifyText() {
        val cue=SubtitleCue(id="subtitle",text="Tá doido?",startMs=1000,endMs=2200,
            wordTimings=listOf(SubtitleWordTiming(1000,1510),SubtitleWordTiming(1510,2200)))
        val styled=SubtitleStyles.applyToCue(cue,preset)
        val first=SubtitleAnimations.frame(styled,1250)
        val second=SubtitleAnimations.frame(styled,1900)
        assertEquals("TÁ DOIDO?",first.text)
        assertEquals("TÁ",first.highlighted?.let {first.text.substring(it)})
        assertEquals("DOIDO?",second.highlighted?.let{second.text.substring(it)})
        assertEquals(cue.id,styled.id)
        assertEquals(cue.text,styled.text)
        assertEquals(cue.wordTimings,styled.wordTimings)
        assertEquals(SubtitleAnimations.KARAOKE_BADGE,SubtitleCue.read(styled.json()).animation)
    }

    @Test fun generatedPinkKaraokeKeepsExistingTimingAndPlacesCaptionLikeVideo() {
        val cue=SubtitleCue(text="Frase curta",startMs=0,endMs=1000,
            wordTimings=listOf(SubtitleWordTiming(0,500),SubtitleWordTiming(500,1000)))
        val pink=SubtitleAppearancePolicy.generated(cue,SubtitleAppearanceMode.KARAOKE_ROSA,22)
        assertEquals(22,pink.sizeSp)
        assertEquals(.79f,pink.y,.001f)
        assertEquals(SubtitleAnimations.KARAOKE_BADGE,pink.animation)
        assertEquals(cue.wordTimings,pink.wordTimings)
        assertEquals(cue.text,pink.text)
    }
}
