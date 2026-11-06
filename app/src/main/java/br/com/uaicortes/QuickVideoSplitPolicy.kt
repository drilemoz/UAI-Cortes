package br.com.uaicortes

/** Cálculos exatos em milissegundos. Não depende do Media3 nem do estado da UI. */
internal data class QuickVideoPart(val index: Int, val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = endMs - startMs
}

internal object QuickVideoSplitPolicy {
    const val SHORT_MS = 9_500L
    const val TEN_SECONDS_MS = 10_000L
    const val MAX_PARTS = 180
    private val unsafeFileName = Regex("[^A-Za-z0-9_-]+")

    fun parts(durationMs: Long, partLengthMs: Long): List<QuickVideoPart> {
        require(durationMs > 0L) { "A duração do vídeo não pôde ser identificada." }
        require(partLengthMs == SHORT_MS || partLengthMs == TEN_SECONDS_MS) {
            "Selecione partes de 9,5 ou 10 segundos."
        }
        val count = 1L + (durationMs - 1L) / partLengthMs
        require(count <= MAX_PARTS) {
            "O vídeo produziria $count partes. O limite por tarefa é $MAX_PARTS; divida um trecho menor."
        }
        return (0 until count.toInt()).map { i ->
            val start = i.toLong() * partLengthMs
            QuickVideoPart(i + 1, start, minOf(durationMs, start + partLengthMs))
        }
    }

    fun fileBase(originalName: String): String = originalName.substringBeforeLast('.', originalName)
        .replace(unsafeFileName, "_").trim('_').take(48).ifBlank { "UaiCortes" }

    fun partName(base: String, session: String, index: Int, extension: String): String {
        require(extension == "mp4" || extension == "m4a")
        return "${fileBase(base)}_${session}_Parte_${index.toString().padStart(3, '0')}.$extension"
    }
}
