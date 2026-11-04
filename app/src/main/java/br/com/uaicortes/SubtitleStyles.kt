package br.com.uaicortes

/** Aparências sem efeitos externos. Cores no formato ARGB, persistidas por legenda.
 * O estilo clássico reproduz o comportamento de projetos salvos antes da v0.1.27.
 */
data class SubtitleStylePreset(
    val name: String, val foreground: Int, val background: Int, val bold: Boolean = true,
    val outlineWidth: Int = 0, val outlineColor: Int = 0xFF000000.toInt(),
    val shadowOffsetDp: Int? = null, val fontFamily: String = "sans", val italic: Boolean = false,
    val highlightColor: Int = SubtitleWordEffects.DEFAULT_HIGHLIGHT,
    val animation: String? = null
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
        SubtitleStylePreset("Clássico", 0xFFFFFFFF.toInt(), DEFAULT_BACKGROUND, animation=SubtitleAnimations.FADE),
        SubtitleStylePreset("Destaque", 0xFFFFFF00.toInt(), DARK_BACKGROUND, animation=SubtitleAnimations.POP),
        SubtitleStylePreset("Ciano", 0xFF00FFFF.toInt(), DARK_BACKGROUND, animation=SubtitleAnimations.SLIDE),
        SubtitleStylePreset("Limpo", 0xFFFFFFFF.toInt(), NO_BACKGROUND, false, animation=SubtitleAnimations.FADE),
        SubtitleStylePreset("Branco forte", -1, NO_BACKGROUND,
            outlineWidth=4,shadowOffsetDp=2,animation=SubtitleAnimations.POP),
        SubtitleStylePreset("Amarelo pop", 0xFFFFFF00.toInt(),NO_BACKGROUND,
            outlineWidth=4,shadowOffsetDp=3,animation=SubtitleAnimations.BOUNCE),
        SubtitleStylePreset("Karaokê azul", -1, NO_BACKGROUND,
            outlineWidth=3,highlightColor=0xFF00CCFF.toInt(),animation=SubtitleAnimations.KARAOKE),
        SubtitleStylePreset("Karaokê vermelho", -1,NO_BACKGROUND,
            outlineWidth=3,highlightColor=0xFFFF2828.toInt(),animation=SubtitleAnimations.KARAOKE),
        SubtitleStylePreset("Energia roxa",0xFFFFFFFF.toInt(),0x990F0B25.toInt(),
            outlineWidth=2,highlightColor=0xFFD549FF.toInt(),animation=SubtitleAnimations.PULSE),
        SubtitleStylePreset("Itálico amarelo",-1,NO_BACKGROUND,
            outlineWidth=3,italic=true,highlightColor=0xFFFFFF00.toInt(),animation=SubtitleAnimations.ZOOM_OUT),
        SubtitleStylePreset("Cinemático",-1,NO_BACKGROUND,
            outlineWidth=2,shadowOffsetDp=4,fontFamily=FONT_SERIF,animation=SubtitleAnimations.FADE),
        SubtitleStylePreset("Etiqueta", 0xFF000000.toInt(), LIGHT_BACKGROUND,animation=SubtitleAnimations.TYPEWRITER),
        // Modelos novos usam as MESMAS curvas na prévia Compose e no Media3.
        SubtitleStylePreset("Neon verde", 0xFF9DFF57.toInt(), NO_BACKGROUND,
            outlineWidth=3,shadowOffsetDp=2,animation=SubtitleAnimations.ELASTIC),
        SubtitleStylePreset("Subida limpa", -1, NO_BACKGROUND,
            outlineWidth=2,shadowOffsetDp=2,animation=SubtitleAnimations.RISE),
        SubtitleStylePreset("Caiu na cena", 0xFFFFFF53.toInt(), NO_BACKGROUND,
            outlineWidth=3,animation=SubtitleAnimations.DROP),
        SubtitleStylePreset("Esquerda pop", -1, 0xBB142638.toInt(),
            outlineWidth=2,animation=SubtitleAnimations.SLIDE_LEFT),
        SubtitleStylePreset("Direita neon", 0xFFB6F3FF.toInt(), NO_BACKGROUND,
            outlineWidth=3,animation=SubtitleAnimations.SLIDE_RIGHT),
        SubtitleStylePreset("Uma palavra", 0xFFFFFFFF.toInt(), DARK_BACKGROUND,
            highlightColor=0xFFFFFF00.toInt(),animation=SubtitleAnimations.WORD_REVEAL),
        SubtitleStylePreset("Flutuante", 0xFFFFFFFF.toInt(), 0x990B183A.toInt(),
            shadowOffsetDp=3,animation=SubtitleAnimations.FLOAT),
        SubtitleStylePreset("Balanceado", 0xFFFFE2FD.toInt(), NO_BACKGROUND,
            outlineWidth=2,animation=SubtitleAnimations.SWAY),
        SubtitleStylePreset("Reação", 0xFFFEFB6B.toInt(), NO_BACKGROUND,
            outlineWidth=4,animation=SubtitleAnimations.SOFT_SHAKE),
        SubtitleStylePreset("Ritmo rosa", 0xFFFFA5E7.toInt(), NO_BACKGROUND,
            outlineWidth=3,shadowOffsetDp=2,animation=SubtitleAnimations.PULSE)
    )

    fun matches(cue: SubtitleCue, preset: SubtitleStylePreset): Boolean =
        cue.color == preset.foreground && cue.backgroundColor == preset.background &&
        cue.bold == preset.bold && cue.outlineWidth == preset.outlineWidth &&
        cue.outlineColor == preset.outlineColor &&
        (preset.shadowOffsetDp == null || cue.shadowOffsetDp == preset.shadowOffsetDp) &&
        cue.fontFamily == preset.fontFamily && cue.italic == preset.italic &&
        cue.highlightColor == preset.highlightColor &&
        (preset.animation == null || cue.animation == preset.animation)

    fun applyToCue(cue: SubtitleCue, preset: SubtitleStylePreset): SubtitleCue =
        cue.copy(color=preset.foreground,backgroundColor=preset.background,bold=preset.bold,
            outlineWidth=preset.outlineWidth,outlineColor=preset.outlineColor,
            shadowOffsetDp=preset.shadowOffsetDp ?: cue.shadowOffsetDp,fontFamily=preset.fontFamily,
            italic=preset.italic,highlightColor=preset.highlightColor,
            animation=preset.animation ?: cue.animation)

    data class CopyOptions(val appearance: Boolean = true, val position: Boolean = false,
        val animation: Boolean = true, val wordColors: Boolean = false) {
        fun summary(count: Int): String {
            val parts = buildList {
                if (appearance) add("estilo")
                if (animation) add("animação")
                if (position) add("posição")
                if (appearance && wordColors) add("cores de palavras iguais")
            }
            return "Aplicado: ${parts.joinToString(", ")} em $count legendas"
        }
    }

    /** Cópia explícita por palavra normalizada e sua ocorrência, jamais por índice bruto.
     * Mantém as cores da legenda de destino nas palavras sem correspondência.
     */
    fun matchingWordColors(source: SubtitleCue, target: SubtitleCue): List<SubtitleWordColor> {
        fun words(cue: SubtitleCue): List<String> = SubtitleWordEffects.wordRanges(cue.text)
            .map { range -> cue.text.substring(range).trim { !it.isLetterOrDigit() }.lowercase() }
        val srcWords = words(source)
        val dstWords = words(target)
        val colored = source.wordColors.associateBy { it.index }
        val mappings = mutableMapOf<String, MutableList<Int?>>()
        srcWords.forEachIndexed { index, word ->
            if (word.isNotBlank()) {
                // Mantém a ordem inclusive quando a primeira ocorrência não tem cor.
                mappings.getOrPut(word) { mutableListOf() }.add(colored[index]?.color)
            }
        }
        val visited = mutableMapOf<String, Int>()
        val result = target.wordColors.associateBy { it.index }.toMutableMap()
        dstWords.forEachIndexed { index, word ->
            val colors = mappings[word].orEmpty()
            val ordinal = visited.getOrDefault(word, 0)
            visited[word] = ordinal + 1
            if (ordinal < colors.size && colors[ordinal] != null)
                result[index] = SubtitleWordColor(index, colors[ordinal]!!)
        }
        return result.values.sortedBy { it.index }
    }

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
            wordColors=if (options.wordColors) matchingWordColors(source,cue) else cue.wordColors)
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
