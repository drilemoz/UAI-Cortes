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
    val encoderName: String?,
    val requestedVideoBitrateBps: Int = 0,
    val presetLabel: String? = null,
    val encodeElapsedMs: Long = -1L,
    val galleryCopyElapsedMs: Long = -1L,
    val requestedFrameWidth: Int = 0,
    val requestedFrameHeight: Int = 0,
    val destinationLabel: String? = null
) {
    val resolution: String get() = if (width > 0 && height > 0) "${width} × ${height}" else "Não informada"
    val effectiveBitrate: String get() = if (videoBitrateBps > 0)
        String.format(Locale.forLanguageTag("pt-BR"), "%.1f Mb/s", videoBitrateBps / 1_000_000.0)
        else "Não informado"
    val requestedBitrate: String get() = if (requestedVideoBitrateBps > 0)
        String.format(Locale.forLanguageTag("pt-BR"), "%.1f Mb/s", requestedVideoBitrateBps / 1_000_000.0)
        else "Não informado"
    /** Taxa baixa não prova perda de qualidade: cenas estáticas comprimem melhor. */
    val bitrateWellBelowRequest: Boolean get() = requestedVideoBitrateBps > 0 &&
        videoBitrateBps > 0 && videoBitrateBps.toLong() * 2L < requestedVideoBitrateBps.toLong()
    val fileSize: String get() = if (fileBytes > 0L)
        String.format(Locale.forLanguageTag("pt-BR"), "%.1f MiB", fileBytes / (1024.0 * 1024.0))
        else "Não informado"
    // Aproximação derivada de frames / duração reportada; não é FPS nominal garantido.
    val averageFps: String get() = if (frames > 0 && durationMs > 0 && durationMs != Long.MAX_VALUE)
        String.format(Locale.forLanguageTag("pt-BR"), "%.1f (média estimada)", frames * 1000.0 / durationMs)
        else "Não informado"
    /** Metrica real de relógio monotônico; fator estimado com duração reportada. */
    val encodeSpeedFactor: Double? get() = if (
        durationMs > 0L && durationMs != Long.MAX_VALUE && encodeElapsedMs > 0L
    ) durationMs.toDouble() / encodeElapsedMs.toDouble() else null
    private fun formatElapsed(elapsedMs: Long): String = String.format(
        Locale.forLanguageTag("pt-BR"), "%.1f s", elapsedMs / 1000.0
    )
    val fellBackInResolution: Boolean get() = width > 0 && height > 0 && (
        if (requestedFrameWidth > 0 && requestedFrameHeight > 0)
            width != requestedFrameWidth || height != requestedFrameHeight
        else minOf(width, height) != requestedShortSide
    )

    fun lines(): List<String> = buildList {
        val requested = if (requestedFrameWidth > 0 && requestedFrameHeight > 0)
            "${requestedFrameWidth} × ${requestedFrameHeight}" else "lado menor ${requestedShortSide}px"
        add("Resolução obtida: $resolution (solicitada: $requested)")
        destinationLabel?.takeIf { it.isNotBlank() }?.let { add("Destino de exportação: $it") }
        if (requestedVideoBitrateBps > 0) {
            add("Bitrate solicitado: $requestedBitrate" +
                (presetLabel?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""))
        }
        add("Bitrate de vídeo medido: $effectiveBitrate")
        if (bitrateWellBelowRequest)
            add("Nota: bitrate medido abaixo de metade do solicitado. Cenas simples e ajustes do encoder podem explicar; compare visualmente o MP4.")
        add("Taxa de quadros: $averageFps")
        add("Tamanho do arquivo: $fileSize")
        if (encodeElapsedMs >= 0L) {
            add("Tempo de processamento: ${formatElapsed(encodeElapsedMs)}")
            encodeSpeedFactor?.let { factor -> add("Velocidade de exportação: " +
                String.format(Locale.forLanguageTag("pt-BR"), "%.2f× a duração do vídeo (estimada)", factor)) }
        }
        if (galleryCopyElapsedMs >= 0L)
            add("Tempo de cópia para a galeria: ${formatElapsed(galleryCopyElapsedMs)}")
        add("Codec de vídeo: ${videoMimeType?.takeIf { it.isNotBlank() } ?: "Não informado"}")
        if (audioBitrateBps > 0) add("Bitrate de áudio medido: ${audioBitrateBps / 1000} kb/s")
        encoderName?.takeIf { it.isNotBlank() }?.let { add("Codificador: $it") }
        if (fellBackInResolution) add("Aviso: o aparelho/exportador usou uma resolução diferente da solicitada.")
        add("Os valores são da exportação local, antes da recompressão das redes sociais.")
    }
}
