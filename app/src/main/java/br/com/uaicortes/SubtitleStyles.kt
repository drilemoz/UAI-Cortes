package br.com.uaicortes

/** Aparências sem efeitos externos. Cores no formato ARGB, persistidas por legenda.
 * O estilo clássico reproduz o comportamento de projetos salvos antes da v0.1.27.
 */
data class SubtitleStylePreset(
    val name: String, val foreground: Int, val background: Int, val bold: Boolean = true
)

object SubtitleStyles {
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
            backgroundColor = source.backgroundColor, bold = source.bold)
    }
}
