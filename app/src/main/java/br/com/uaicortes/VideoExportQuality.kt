package br.com.uaicortes

/** Perfis de bitrate solicitados ao encoder H.264, sem prometer taxas garantidas. */
internal enum class ExportBitratePreset(val label: String) {
    COMPACT("Compacta"),
    HIGH("Alta"),
    MAXIMUM("Máxima");

    companion object {
        fun fromId(id: String?): ExportBitratePreset =
            values().firstOrNull { it.name == id } ?: HIGH
    }
}

/**
 * Parâmetros solicitados ao Media3. O encoder pode ajustar resolução, taxa e GOP.
 * Um arquivo-fonte pequeno/comprimido não recupera detalhe apenas com maior bitrate.
 */
internal object VideoExportQuality {
    const val AUDIO_BITRATE_BPS: Int = 192_000
    const val MIN_WORKING_SPACE_BYTES: Long = 96L * 1024 * 1024
    const val REQUESTED_KEYFRAME_INTERVAL_SECONDS: Float = 2f

    fun videoBitrateBps(outputShortSide: Int,
                        preset: ExportBitratePreset = ExportBitratePreset.HIGH): Int = when (outputShortSide) {
        720 -> when (preset) {
            ExportBitratePreset.COMPACT -> 5_000_000
            ExportBitratePreset.HIGH -> 8_000_000
            ExportBitratePreset.MAXIMUM -> 12_000_000
        }
        1080 -> when (preset) {
            ExportBitratePreset.COMPACT -> 10_000_000
            ExportBitratePreset.HIGH -> 18_000_000
            ExportBitratePreset.MAXIMUM -> 28_000_000
        }
        else -> throw IllegalArgumentException("Resolução de exportação não suportada: $outputShortSide")
    }

    /**
     * Estimativa conservadora em bytes, com aritmética saturada.
     * Não truncar para 24h: projetos com mais de 24h precisam da reserva proporcional.
     * Divide primeiro para impedir overflow na multiplicação (bps * milissegundos).
     */
    private fun estimatedBytes(durationMs: Long, outputShortSide: Int, withAudio: Boolean,
                               preset: ExportBitratePreset): Long {
        require(durationMs >= 0L)
        val bitsPerSecond = videoBitrateBps(outputShortSide, preset).toLong() +
            (if (withAudio) AUDIO_BITRATE_BPS else 0)
        val whole = durationMs / 8_000L
        val remainder = durationMs % 8_000L
        if (whole > Long.MAX_VALUE / bitsPerSecond) return Long.MAX_VALUE
        val primary = whole * bitsPerSecond
        val fractional = remainder * bitsPerSecond / 8_000L
        return if (primary > Long.MAX_VALUE - fractional) Long.MAX_VALUE else primary + fractional
    }

    /** Reserva estimada para MP4 temporário + cópia MediaStore + margem de trabalho. */
    fun minimumFreeBytes(durationMs: Long, outputShortSide: Int, withAudio: Boolean,
                         preset: ExportBitratePreset = ExportBitratePreset.HIGH): Long {
        val bytes = estimatedBytes(durationMs, outputShortSide, withAudio, preset)
        val safeLimit = (Long.MAX_VALUE - MIN_WORKING_SPACE_BYTES) / 2L
        return if (bytes > safeLimit) Long.MAX_VALUE
            else bytes * 2L + MIN_WORKING_SPACE_BYTES
    }

    /** Estimativa para o projeto completo; satura em Int.MAX_VALUE MiB em vez de transbordar. */
    fun estimatedMiBForDuration(durationMs: Long, outputShortSide: Int, withAudio: Boolean,
                                preset: ExportBitratePreset = ExportBitratePreset.HIGH): Int {
        val bytes = estimatedBytes(durationMs, outputShortSide, withAudio, preset)
        val mebibyte = 1024L * 1024L
        val rounded = bytes / mebibyte + if (bytes % mebibyte == 0L) 0L else 1L
        return rounded.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    /** Referência de armazenamento por minuto, não tamanho exato do arquivo exportado. */
    fun estimatedMiBPerMinute(outputShortSide: Int, withAudio: Boolean,
                              preset: ExportBitratePreset = ExportBitratePreset.HIGH): Int {
        val bitsPerSecond = videoBitrateBps(outputShortSide, preset).toLong() +
            (if (withAudio) AUDIO_BITRATE_BPS else 0)
        val bytes = (bitsPerSecond * 60L) / 8L
        return ((bytes + 1024L * 1024L - 1L) / (1024L * 1024L)).toInt()
    }
}
