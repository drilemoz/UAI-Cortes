package br.com.uaicortes

/** Aparência inicial da legenda. Não altera texto, tempos, personagem ou marcações por palavra. */
internal enum class SubtitleAppearanceMode { CLASSIC_SRT, KARAOKE_IA, KARAOKE_ROSA }

internal const val DEFAULT_SUBTITLE_SIZE_SP = 14

internal object SubtitleAppearancePolicy {
    fun defaultSize(): Int = DEFAULT_SUBTITLE_SIZE_SP

    fun generated(cue: SubtitleCue, mode: SubtitleAppearanceMode, requestedSize: Int): SubtitleCue {
        val size = requestedSize.coerceIn(12, 30)
        return when (mode) {
            SubtitleAppearanceMode.CLASSIC_SRT -> cue.copy(
                sizeSp = size,
                color = -1,
                backgroundColor = SubtitleStyles.DEFAULT_BACKGROUND,
                outlineWidth = 0,
                shadowOffsetDp = 0,
                animation = SubtitleAnimations.NONE
            )
            SubtitleAppearanceMode.KARAOKE_IA -> cue.copy(
                sizeSp = size,
                color = 0xFFFFFFFF.toInt(),
                backgroundColor = SubtitleStyles.NO_BACKGROUND,
                outlineWidth = 2,
                shadowOffsetDp = 0,
                animation = SubtitleAnimations.KARAOKE
            )
            SubtitleAppearanceMode.KARAOKE_ROSA -> cue.copy(
                sizeSp=size, x=.5f,y=.79f,
                color=0xFFFFFFFF.toInt(), backgroundColor=SubtitleStyles.NO_BACKGROUND,
                bold=true,outlineWidth=2,outlineColor=0xFF000000.toInt(),
                shadowOffsetDp=0,shadowColor=0xFF000000.toInt(),
                highlightColor=0xFFE81050.toInt(),
                textCase=br.com.uaicortes.subtitles.core.SubtitleTextCasePolicy.UPPER,
                animation=SubtitleAnimations.KARAOKE_BADGE
            )
        }
    }
}
