package br.com.uaicortes

import java.util.Locale

/** Medição monotônica suavizada; não inventa velocidade antes de ter bytes confirmados. */
internal class UploadRateEstimator {
    private var lastBytes = 0L
    private var lastTime = -1L
    private var startedAt = -1L
    private var rate = 0.0

    fun update(sent: Long, total: Long, elapsedMs: Long): Pair<Double, Long?> {
        if (total <= 0 || sent < 0 || sent > total) return 0.0 to null
        if (startedAt < 0) startedAt = elapsedMs
        if (lastTime >= 0 && elapsedMs > lastTime && sent > lastBytes) {
            val deltaMs = elapsedMs - lastTime
            if (deltaMs >= 500L) {
                val instant = (sent - lastBytes).toDouble() * 1000.0 / deltaMs
                rate = if (rate == 0.0) instant else rate * 0.7 + instant * 0.3
                lastBytes = sent
                lastTime = elapsedMs
            }
        } else if (lastTime < 0 || sent < lastBytes) {
            if (sent < lastBytes) rate = 0.0
            lastTime = elapsedMs
            lastBytes = sent
        }
        val eta = if (rate > 0.0 && elapsedMs - startedAt >= 1000L && sent < total)
            kotlin.math.ceil((total - sent).toDouble() / rate).toLong().coerceAtMost(604800L)
        else null
        return rate to eta
    }
}

internal object UploadReceiptFormat {
    fun bytes(value: Long): String = if (value >= 1_048_576)
        String.format(Locale.ROOT, "%.1f MB", value.toDouble() / 1_048_576.0)
    else "${value.coerceAtLeast(0L) / 1024L} KB"
    fun eta(seconds: Long?): String = when {
        seconds == null -> "Calculando tempo restante..."
        seconds >= 3600 -> "~${seconds / 3600} h ${seconds % 3600 / 60} min restantes"
        seconds >= 60 -> "~${seconds / 60} min ${seconds % 60} s restantes"
        else -> "~$seconds s restantes"
    }
}

