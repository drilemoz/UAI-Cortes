package br.com.uaicortes

import br.com.uaicortes.subtitles.core.SubtitleTextCasePolicy

/** Presets de estreia do Subtitle Studio. Presets legados permanecem acessíveis. */
internal object SubtitleStudioPresets {
    val recommended: List<SubtitleStylePreset> = listOf(
        SubtitleStylePreset("Pop moderno", -1, SubtitleStyles.NO_BACKGROUND,
            outlineWidth=4, shadowOffsetDp=2, highlightColor=0xFFFFD840.toInt(),
            animation=SubtitleAnimations.SOFT_POP, textCase=SubtitleTextCasePolicy.UPPER),
        SubtitleStylePreset("Karaokê limpo", -1, SubtitleStyles.NO_BACKGROUND,
            outlineWidth=2, shadowOffsetDp=2, highlightColor=0xFF54DAFF.toInt(),
            animation=SubtitleAnimations.KARAOKE, textCase=SubtitleTextCasePolicy.UPPER),
        SubtitleStylePreset("Cinematográfico", 0xFFF8F2E4.toInt(), SubtitleStyles.NO_BACKGROUND,
            bold=false, shadowOffsetDp=3, fontFamily=SubtitleStyles.FONT_SERIF,
            animation=SubtitleAnimations.CINEMATIC),
        SubtitleStylePreset("Humor e reação", 0xFFFFEA87.toInt(), 0xCC242033.toInt(),
            outlineWidth=2, shadowOffsetDp=1, animation=SubtitleAnimations.IMPACT,
            textCase=SubtitleTextCasePolicy.UPPER),
        SubtitleStylePreset("Documentário", -1, 0xB8000000.toInt(), bold=false,
            shadowOffsetDp=1, animation=SubtitleAnimations.SOFT_FADE),
        SubtitleStylePreset("Faixa inferior", 0xFF161616.toInt(), 0xF1F1F1F1.toInt(),
            animation=SubtitleAnimations.GLIDE),
        SubtitleStylePreset("Diário de viagem", 0xFFF4F9F9.toInt(), 0x880A2E37.toInt(),
            shadowOffsetDp=2, fontFamily=SubtitleStyles.FONT_SERIF,
            highlightColor=0xFFFFD780.toInt(), animation=SubtitleAnimations.CINEMATIC),
        SubtitleStylePreset("Impacto", 0xFFFFFF6A.toInt(), SubtitleStyles.NO_BACKGROUND,
            outlineWidth=4, shadowOffsetDp=3, animation=SubtitleAnimations.IMPACT,
            textCase=SubtitleTextCasePolicy.UPPER)
    )
    init { require(recommended.map { it.name }.distinct().size == recommended.size) }
}
