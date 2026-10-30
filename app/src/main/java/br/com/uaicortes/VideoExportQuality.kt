package br.com.uaicortes

/** Parâmetros de exportação H.264 solicitados ao encoder, não garantidos pelo hardware.
 *  Não inventar qualidade recuperada: vídeos-fonte pequenos/ruins continuam limitados pela origem.
 */
internal object VideoExportQuality {
    const val AUDIO_BITRATE_BPS: Int = 192_000
    const val MIN_WORKING_SPACE_BYTES: Long = 96L * 1024 * 1024

    fun videoBitrateBps(outputHeight: Int): Int = when (outputHeight) {
        720 -> 8_000_000
        1080 -> 18_000_000
        else -> throw IllegalArgumentException("Resolução de exportação não suportada: $outputHeight")
    }

    /** Espaço para MP4 temporário e cópia no MediaStore (mesmo volume é possível).
     *  Margem preventiva, não garantia de que o encoder obedecerá à taxa solicitada.
     */
    fun minimumFreeBytes(durationMs: Long, outputHeight: Int, withAudio: Boolean): Long {
        require(durationMs >= 0L)
        val bitsPerSecond = videoBitrateBps(outputHeight).toLong() +
            (if (withAudio) AUDIO_BITRATE_BPS else 0)
        // Uma multiplicação conservadora de 24h (projetos reais devem ser menores).
        val boundedMs = durationMs.coerceAtMost(86_400_000L)
        val mp4Estimate = bitsPerSecond * boundedMs / 8_000L
        return (mp4Estimate * 2L) + MIN_WORKING_SPACE_BYTES
    }

    /** Apenas referência de armazenamento por minuto, sem representar o tamanho final exato. */
    fun estimatedMiBPerMinute(outputHeight: Int, withAudio: Boolean): Int {
        val bitsPerSecond = videoBitrateBps(outputHeight).toLong() +
            (if (withAudio) AUDIO_BITRATE_BPS else 0)
        val bytes = (bitsPerSecond * 60L) / 8L
        return ((bytes + 1024L * 1024L - 1L) / (1024L * 1024L)).toInt()
    }
}
