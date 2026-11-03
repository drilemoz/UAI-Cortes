package br.com.uaicortes

/** Orientação de duração para TTS por trecho. Não corta WAV nem presume duração exata da API. */
internal object NarrationDurationAssist {
    const val MIN_TARGET_MS = 500L
    const val MAX_TARGET_MS = 60_000L
    const val STEP_MS = 250L

    fun defaultTarget(startMs: Long, subtitleEndMs: Long, projectDurationMs: Long): Long =
        (subtitleEndMs - startMs).coerceIn(MIN_TARGET_MS,
            (projectDurationMs - startMs).coerceAtMost(MAX_TARGET_MS).coerceAtLeast(MIN_TARGET_MS))

    fun adjust(currentMs: Long, deltaMs: Long, startMs: Long, projectDurationMs: Long): Long {
        val max = (projectDurationMs - startMs).coerceAtMost(MAX_TARGET_MS)
        if (max < MIN_TARGET_MS) return 0L // sem espaço para uma meta válida
        return (currentMs + deltaMs).coerceIn(MIN_TARGET_MS, max)
    }

    /** Apenas um pedido prosódico: a duração final SEMPRE virá do WAV medido. */
    fun instruction(targetMs: Long): String {
        require(targetMs in MIN_TARGET_MS..MAX_TARGET_MS)
        val seconds = "%.2f".format(java.util.Locale.ROOT, targetMs / 1000.0)
        return "Procure falar este trecho completo em aproximadamente $seconds segundos, " +
            "com ritmo natural e claro. Nunca omita palavras ou corte a frase. " +
            "A duração é uma meta aproximada, não uma exigência rígida."
    }

    fun difference(actualMs: Long, targetMs: Long): Long = actualMs - targetMs
}
