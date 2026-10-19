package br.com.uaicortes

/** Linha do tempo, independente da interface e do formato do arquivo. */
object OverlayTiming {
    /** Novo PiP começa no último término, mesmo com o cursor em outro lugar. */
    fun nextStart(overlays: List<Overlay>, totalMs: Long): Long? {
        if (totalMs <= 0L) return null
        val end = overlays.maxOfOrNull { it.startAtMs + it.durationMs } ?: 0L
        return end.takeIf { it <= totalMs - minOf(500L, totalMs) }
    }

    /** Se acabou o tempo, só inserir em período concomitante mediante autorização. */
    fun suggestedOverlapStart(overlays: List<Overlay>, totalMs: Long): Long =
        overlays.maxByOrNull { it.startAtMs + it.durationMs }?.startAtMs
            ?.coerceIn(0L, (totalMs - 1L).coerceAtLeast(0L)) ?: 0L
}

/** Aceita mm:ss, mm:ss.d, mm:ss.ddd, hh:mm:ss e segundos isolados. */
object TimeInput {
    fun parseMs(value: String): Long? {
        val text = value.trim().replace(',', '.')
        if (text.isEmpty()) return null
        val parts = text.split(':')
        if (parts.size !in 1..3 || parts.any { it.isEmpty() }) return null
        val last = parts.last().split('.')
        if (last.size !in 1..2 || last[0].isEmpty() ||
            (last.size == 2 && (last[1].length !in 1..3 || !last[1].all(Char::isDigit)))) return null
        val seconds = last[0].toLongOrNull() ?: return null
        if (seconds < 0 || (parts.size >= 2 && seconds >= 60)) return null
        val minutes = if (parts.size >= 2) parts[parts.size - 2].toLongOrNull() ?: return null else 0L
        val hours = if (parts.size == 3) parts[0].toLongOrNull() ?: return null else 0L
        if (minutes < 0 || hours < 0 || (parts.size == 3 && minutes >= 60)) return null
        val fraction = if (last.size == 2) last[1].padEnd(3, '0').toLong() else 0L
        return runCatching { Math.addExact(Math.addExact(
            Math.multiplyExact(Math.addExact(Math.multiplyExact(hours, 60L), minutes), 60000L),
            Math.multiplyExact(seconds, 1000L)), fraction) }.getOrNull()
    }
}
