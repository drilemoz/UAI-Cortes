package br.com.uaicortes

/** Cálculos exatos em milissegundos. Não depende do Media3 nem do estado da UI. */
internal data class QuickVideoPart(val index: Int, val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = endMs - startMs
}

internal object QuickVideoSplitPolicy {
    const val SHORT_MS = 9_500L
    const val TEN_SECONDS_MS = 10_000L
    const val THIRTY_SECONDS_MS = 30_000L
    const val ONE_MINUTE_MS = 60_000L
    const val ONE_MINUTE_FIVE_MS = 65_000L
    const val TWO_MINUTES_MS = 120_000L
    const val SHORTS_MARGIN_MS = 179_000L
    const val THREE_MINUTES_MS = 180_000L
    const val FOUR_MINUTES_MS = 240_000L
    const val EIGHT_MINUTES_MS = 480_000L
    const val MAX_CUSTOM_MS = 86_399_000L // 23:59:59; valores inteiros em segundos
    const val MAX_PARTS = 180
    private val unsafeFileName = Regex("[^A-Za-z0-9_-]+")

    fun parts(durationMs: Long, partLengthMs: Long): List<QuickVideoPart> {
        require(durationMs > 0L) { "A duração do vídeo não pôde ser identificada." }
        require(partLengthMs == SHORT_MS ||
            (partLengthMs in 1_000L..MAX_CUSTOM_MS && partLengthMs % 1_000L == 0L)) {
            "Informe partes de 9,5 s ou uma duração entre 1 segundo e 23h59min59s."
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

    /** Horas, minutos e segundos; retorna null em vez de aplicar um valor inválido. */
    fun customDurationMs(hours: Int?, minutes: Int?, seconds: Int?): Long? {
        if (hours == null || minutes == null || seconds == null ||
            hours !in 0..23 || minutes !in 0..59 || seconds !in 0..59) return null
        val total = (hours.toLong() * 3600L + minutes * 60L + seconds) * 1000L
        return total.takeIf { it in 1_000L..MAX_CUSTOM_MS }
    }

    /** Exibe horas quando necessário e preserva frações de segundo (9,5 s). */
    fun timeLabel(milliseconds: Long): String {
        val ms = milliseconds.coerceAtLeast(0L)
        val total = ms / 1000L
        val hours = total / 3600L
        val minutes = (total / 60L) % 60L
        val seconds = total % 60L
        val base = if (hours > 0L) {
            java.lang.String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            java.lang.String.format(java.util.Locale.ROOT, "%02d:%02d", total / 60L, seconds)
        }
        return if (ms % 1000L == 0L) base else
            base + java.lang.String.format(java.util.Locale.ROOT, ".%03d", ms % 1000L)
    }

    fun fileBase(originalName: String): String = originalName.substringBeforeLast('.', originalName)
        .replace(unsafeFileName, "_").trim('_').take(48).ifBlank { "UaiCortes" }

    fun partName(base: String, session: String, index: Int, extension: String): String {
        require(extension == "mp4" || extension == "m4a")
        return "${fileBase(base)}_${session}_Parte_${index.toString().padStart(3, '0')}.$extension"
    }
}
