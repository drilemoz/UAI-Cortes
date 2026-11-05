package br.com.uaicortes

/** Preferências de saída, não certificação/garantia de aceitação por uma plataforma. */
internal enum class ExportDestination(val label: String) {
    ORIGINAL("Original"), SHORTS("Shorts"), REELS("Reels"), TIKTOK("TikTok");

    val isVertical: Boolean get() = this != ORIGINAL

    companion object {
        fun fromId(value: String?): ExportDestination =
            values().firstOrNull { it.name == value } ?: ORIGINAL
    }
}

/** Evita corte automático sem uma escolha explícita do usuário. */
internal enum class VerticalFrameMode(val label: String) {
    FIT("Mostrar tudo"), CENTER_CROP("Preencher tela");

    companion object {
        fun fromId(value: String?): VerticalFrameMode =
            values().firstOrNull { it.name == value } ?: FIT
    }
}

/** Regras puras de resolução, compartilhadas por interface, exportador e testes. */
internal object SocialExportPolicy {
    fun dimensions(destination: ExportDestination, shortSide: Int): Pair<Int, Int>? {
        require(shortSide in listOf(360, 480, 720, 1080)) { "Resolução não suportada: $shortSide" }
        // A dimensão vertical solicitada deve ser par para codificação H.264.
        val vertical = ((shortSide * 16.0 / 9.0 + 1.0) / 2.0).toInt() * 2
        return if (destination.isVertical) shortSide to vertical else null
    }

    fun requestedLabel(destination: ExportDestination, shortSide: Int): String =
        dimensions(destination, shortSide)?.let { "${it.first} × ${it.second} (9:16)" }
            ?: "Lado menor: ${shortSide}px · proporção original"

    fun recommendedBitrate(destination: ExportDestination): ExportBitratePreset =
        if (destination.isVertical) ExportBitratePreset.COMPACT else ExportBitratePreset.HIGH

    fun framingDescription(mode: VerticalFrameMode): String = when (mode) {
        VerticalFrameMode.FIT ->
            "Mostra toda a imagem. Se a origem não for 9:16, acrescenta barras para preservar o conteúdo."
        VerticalFrameMode.CENTER_CROP ->
            "Preenche 9:16 sem distorcer, cortando o centro da imagem. Pessoas, textos e objetos nas bordas podem desaparecer."
    }
}
