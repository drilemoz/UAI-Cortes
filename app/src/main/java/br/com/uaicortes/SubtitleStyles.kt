package br.com.uaicortes

/** Aparências sem efeitos externos. Cores no formato ARGB, persistidas por legenda.
 * O estilo clássico reproduz o comportamento de projetos salvos antes da v0.1.27.
 */
data class SubtitleStylePreset(
    val name: String, val foreground: Int, val background: Int, val bold: Boolean = true
)

object SubtitleStyles {
    const val FONT_SANS = "sans"
    const val FONT_SERIF = "serif"
    const val FONT_MONO = "mono"
    val fonts: List<Pair<String, String>> = listOf(
        FONT_SANS to "Moderna", FONT_SERIF to "Serifada", FONT_MONO to "Monoespaçada"
    )
    fun normalizeFont(value: String): String =
        if (value == FONT_SERIF || value == FONT_MONO) value else FONT_SANS

    // Defaults keep pre-v0.1.29 text unchanged when deserializing existing projects.
    const val ALIGN_LEFT = "left"
    const val ALIGN_CENTER = "center"
    const val ALIGN_RIGHT = "right"
    val alignments: List<Pair<String, String>> = listOf(
        ALIGN_LEFT to "Esquerda", ALIGN_CENTER to "Centro", ALIGN_RIGHT to "Direita"
    )
    fun normalizeAlignment(value: String): String =
        if (value == ALIGN_LEFT || value == ALIGN_RIGHT) value else ALIGN_CENTER

    fun normalizeLineSpacing(value: Int): Int = value.coerceIn(100, 160)
    fun normalizeShadowColor(value: Int): Int =
        if (value == 0xFFFFFFFF.toInt()) value else 0xFF000000.toInt()

    val DEFAULT_BACKGROUND: Int = 0xAA000000.toInt()
    val DARK_BACKGROUND: Int = 0xE6000000.toInt()
    val LIGHT_BACKGROUND: Int = 0xE6FFFFFF.toInt()
    const val NO_BACKGROUND: Int = 0x00000000

    val presets = listOf(
        SubtitleStylePreset("Clássico", 0xFFFFFFFF.toInt(), DEFAULT_BACKGROUND),
        SubtitleStylePreset("Destaque", 0xFFFFFF00.toInt(), DARK_BACKGROUND),
        SubtitleStylePreset("Ciano", 0xFF00FFFF.toInt(), DARK_BACKGROUND),
        SubtitleStylePreset("Limpo", 0xFFFFFFFF.toInt(), NO_BACKGROUND, false),
        SubtitleStylePreset("Etiqueta", 0xFF000000.toInt(), LIGHT_BACKGROUND)
    )

    fun applyToCue(cue: SubtitleCue, preset: SubtitleStylePreset): SubtitleCue =
        cue.copy(color = preset.foreground, backgroundColor = preset.background, bold = preset.bold)

    /** Copia somente aparência, sem reescrever texto, posição, IDs ou sincronização. */
    fun applyToAll(cues: List<SubtitleCue>, source: SubtitleCue): List<SubtitleCue> = cues.map {
        it.copy(color = source.color, sizeSp = source.sizeSp,
            backgroundColor = source.backgroundColor, bold = source.bold,
            fontFamily = source.fontFamily, outlineWidth = source.outlineWidth,
            outlineColor = source.outlineColor,
            shadowOffsetDp = source.shadowOffsetDp, shadowColor = source.shadowColor,
            lineSpacingPercent = source.lineSpacingPercent, textAlignment = source.textAlignment)
    }
}
