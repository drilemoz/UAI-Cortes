package br.com.uaicortes

/** Aparência inicial da legenda. Não altera texto, tempos, personagem ou marcações por palavra. */
internal enum class SubtitleAppearanceMode { CLASSIC_SRT, KARAOKE_IA }

internal object SubtitleAppearancePolicy {
    fun defaultSize(mode: SubtitleAppearanceMode): Int = when (mode) {
        SubtitleAppearanceMode.CLASSIC_SRT -> 12
        SubtitleAppearanceMode.KARAOKE_IA -> 20
    }

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
                outlineWidth = 3,
                shadowOffsetDp = 2,
                animation = SubtitleAnimations.KARAOKE
            )
        }
    }
}
