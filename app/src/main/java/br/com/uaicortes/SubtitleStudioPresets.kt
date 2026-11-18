package br.com.uaicortes

import br.com.uaicortes.subtitles.core.SubtitleTextCasePolicy

/** Os cinco modelos de fábrica exibidos. Estilos antigos já aplicados continuam renderizáveis. */
internal object SubtitleStudioPresets {
    val recommended: List<SubtitleStylePreset> = listOf(
        SubtitleStylePreset("Karaokê rosa do vídeo", -1, SubtitleStyles.NO_BACKGROUND,
            outlineWidth = 4, shadowOffsetDp = 1, fontFamily = SubtitleStyles.FONT_HEAVY,
            highlightColor = SubtitleAppearancePolicy.ROSA_HIGHLIGHT,
            animation = SubtitleAnimations.KARAOKE_BADGE, textCase = SubtitleTextCasePolicy.UPPER,
            sizeSp = SubtitleAppearancePolicy.ROSA_SIZE_SP),
        SubtitleStylePreset("Uma palavra por vez", -1, SubtitleStyles.NO_BACKGROUND,
            outlineWidth = 2, shadowOffsetDp = 0, animation = SubtitleAnimations.SINGLE_WORD),
        SubtitleStylePreset("Karaokê limpo", -1, SubtitleStyles.NO_BACKGROUND,
            outlineWidth = 2, shadowOffsetDp = 0, highlightColor = 0xFF54DAFF.toInt(),
            animation = SubtitleAnimations.KARAOKE, textCase = SubtitleTextCasePolicy.UPPER),
        SubtitleStylePreset("Cinematográfico", 0xFFF8F2E4.toInt(), SubtitleStyles.NO_BACKGROUND,
            bold = false, shadowOffsetDp = 3, fontFamily = SubtitleStyles.FONT_SERIF,
            animation = SubtitleAnimations.CINEMATIC),
        SubtitleStylePreset("Impacto", 0xFFFFFF6A.toInt(), SubtitleStyles.NO_BACKGROUND,
            outlineWidth = 4, shadowOffsetDp = 3, animation = SubtitleAnimations.IMPACT,
            textCase = SubtitleTextCasePolicy.UPPER)
    )
    init { require(recommended.map { it.name }.distinct().size == recommended.size) }
}
