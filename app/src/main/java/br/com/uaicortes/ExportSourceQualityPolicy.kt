package br.com.uaicortes

/** Um reescalonamento para cima não cria detalhes novos na mídia original. */
internal object ExportSourceQualityPolicy {
    fun warning(minSourceShortSide: Int?, requestedShortSide: Int): String? {
        val source = minSourceShortSide ?: return null
        if (source <= 0 || requestedShortSide <= source) return null
        return "Pelo menos um clipe de origem tem lado menor de ${source}px. " +
            "Exportar em ${requestedShortSide}p amplia esse clipe, mas não recupera nitidez perdida."
    }
}
