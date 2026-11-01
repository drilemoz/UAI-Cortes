package br.com.uaicortes

/** Aparências sem efeitos externos. Cores no formato ARGB, persistidas por legenda.
 * O estilo clássico reproduz o comportamento de projetos salvos antes da v0.1.27.
 */
data class SubtitleStylePreset(
    val name: String, val foreground: Int, val background: Int, val bold: Boolean = true,
    val outlineWidth: Int = 0, val outlineColor: Int = 0xFF000000.toInt(),
    val shadowOffsetDp: Int? = null, val fontFamily: String = "sans", val italic: Boolean = false,
    val highlightColor: Int = SubtitleWordEffects.DEFAULT_HIGHLIGHT
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

    /** Aparências locais, sem serviço externo nem arquivos de fonte de terceiros. */
    val presets = listOf(
        SubtitleStylePreset("Clássico", 0xFFFFFFFF.toInt(), DEFAULT_BACKGROUND),
        SubtitleStylePreset("Destaque", 0xFFFFFF00.toInt(), DARK_BACKGROUND),
        SubtitleStylePreset("Ciano", 0xFF00FFFF.toInt(), DARK_BACKGROUND),
        SubtitleStylePreset("Limpo", 0xFFFFFFFF.toInt(), NO_BACKGROUND, false),
        SubtitleStylePreset("Branco forte", -1, NO_BACKGROUND,
            outlineWidth=4,shadowOffsetDp=2),
        SubtitleStylePreset("Amarelo pop", 0xFFFFFF00.toInt(),NO_BACKGROUND,
            outlineWidth=4,shadowOffsetDp=3),
        SubtitleStylePreset("Karaokê azul", -1, NO_BACKGROUND,
            outlineWidth=3,highlightColor=0xFF00CCFF.toInt()),
        SubtitleStylePreset("Karaokê vermelho", -1,NO_BACKGROUND,
            outlineWidth=3,highlightColor=0xFFFF2828.toInt()),
        SubtitleStylePreset("Energia roxa",0xFFFFFFFF.toInt(),0x990F0B25.toInt(),
            outlineWidth=2,highlightColor=0xFFD549FF.toInt()),
        SubtitleStylePreset("Itálico amarelo",-1,NO_BACKGROUND,
            outlineWidth=3,italic=true,highlightColor=0xFFFFFF00.toInt()),
        SubtitleStylePreset("Cinemático",-1,NO_BACKGROUND,
            outlineWidth=2,shadowOffsetDp=4,fontFamily=FONT_SERIF),
        SubtitleStylePreset("Etiqueta", 0xFF000000.toInt(), LIGHT_BACKGROUND)
    )

    fun matches(cue: SubtitleCue, preset: SubtitleStylePreset): Boolean =
        cue.color == preset.foreground && cue.backgroundColor == preset.background &&
        cue.bold == preset.bold && cue.outlineWidth == preset.outlineWidth &&
        cue.outlineColor == preset.outlineColor &&
        (preset.shadowOffsetDp == null || cue.shadowOffsetDp == preset.shadowOffsetDp) &&
        cue.fontFamily == preset.fontFamily && cue.italic == preset.italic &&
        cue.highlightColor == preset.highlightColor

    fun applyToCue(cue: SubtitleCue, preset: SubtitleStylePreset): SubtitleCue =
        cue.copy(color=preset.foreground,backgroundColor=preset.background,bold=preset.bold,
            outlineWidth=preset.outlineWidth,outlineColor=preset.outlineColor,
            shadowOffsetDp=preset.shadowOffsetDp ?: cue.shadowOffsetDp,fontFamily=preset.fontFamily,
            italic=preset.italic,highlightColor=preset.highlightColor)

    data class CopyOptions(val appearance: Boolean = true, val position: Boolean = false,
        val animation: Boolean = true)

    /** Copia as propriedades selecionadas sem tocar nos textos, IDs ou horários. */
    fun applySelectedToAll(cues: List<SubtitleCue>, source: SubtitleCue,
        options: CopyOptions): List<SubtitleCue> = cues.map { cue ->
        var result = cue
        if (options.appearance) result = result.copy(
            color=source.color, sizeSp=source.sizeSp, backgroundColor=source.backgroundColor,
            bold=source.bold, fontFamily=source.fontFamily,
            outlineWidth=source.outlineWidth,outlineColor=source.outlineColor,
            shadowOffsetDp=source.shadowOffsetDp,shadowColor=source.shadowColor,
            lineSpacingPercent=source.lineSpacingPercent,textAlignment=source.textAlignment,
            italic=source.italic,highlightColor=source.highlightColor,
            wordColors=cue.wordColors)
        if (options.position) result = result.copy(x=source.x,y=source.y)
        if (options.animation) result = result.copy(animation=source.animation,
            animationSpeed=source.animationSpeed)
        result
    }

    /** Copia somente aparência, sem reescrever texto, posição, IDs ou sincronização. */
    fun applyToAll(cues: List<SubtitleCue>, source: SubtitleCue): List<SubtitleCue> = cues.map {
        it.copy(color = source.color, sizeSp = source.sizeSp,
            backgroundColor = source.backgroundColor, bold = source.bold,
            fontFamily = source.fontFamily, outlineWidth = source.outlineWidth,
            outlineColor = source.outlineColor,
            shadowOffsetDp = source.shadowOffsetDp, shadowColor = source.shadowColor,
            lineSpacingPercent = source.lineSpacingPercent, textAlignment = source.textAlignment,
            italic=source.italic,highlightColor=source.highlightColor)
    }
}
