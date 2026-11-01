package br.com.uaicortes

import java.util.Locale

/** Resultado factual do Media3. Valores <=0 ou MIME nulo não viram medições fictícias. */
internal data class ExportQualityReport(
    val requestedShortSide: Int,
    val width: Int,
    val height: Int,
    val videoBitrateBps: Int,
    val audioBitrateBps: Int,
    val durationMs: Long,
    val frames: Int,
    val fileBytes: Long,
    val videoMimeType: String?,
    val encoderName: String?
) {
    val resolution: String get() = if (width > 0 && height > 0) "${width} × ${height}" else "Não informada"
    val effectiveBitrate: String get() = if (videoBitrateBps > 0)
        String.format(Locale.forLanguageTag("pt-BR"), "%.1f Mb/s", videoBitrateBps / 1_000_000.0)
        else "Não informado"
    val fileSize: String get() = if (fileBytes > 0L)
        String.format(Locale.forLanguageTag("pt-BR"), "%.1f MiB", fileBytes / (1024.0 * 1024.0))
        else "Não informado"
    // Aproximação derivada de frames / duração reportada; não é FPS nominal garantido.
    val averageFps: String get() = if (frames > 0 && durationMs > 0 && durationMs != Long.MAX_VALUE)
        String.format(Locale.forLanguageTag("pt-BR"), "%.1f (média estimada)", frames * 1000.0 / durationMs)
        else "Não informado"
    val fellBackInResolution: Boolean get() = width > 0 && height > 0 &&
        minOf(width, height) != requestedShortSide

    fun lines(): List<String> = buildList {
        add("Resolução obtida: $resolution (lado menor solicitado: ${requestedShortSide}px)")
        add("Bitrate de vídeo medido: $effectiveBitrate")
        add("Taxa de quadros: $averageFps")
        add("Tamanho do arquivo: $fileSize")
        add("Codec de vídeo: ${videoMimeType?.takeIf { it.isNotBlank() } ?: "Não informado"}")
        if (audioBitrateBps > 0) add("Bitrate de áudio medido: ${audioBitrateBps / 1000} kb/s")
        encoderName?.takeIf { it.isNotBlank() }?.let { add("Codificador: $it") }
        if (fellBackInResolution) add("Aviso: o aparelho/exportador usou uma resolução diferente da solicitada.")
        add("Os valores são da exportação local, antes da recompressão das redes sociais.")
    }
}
