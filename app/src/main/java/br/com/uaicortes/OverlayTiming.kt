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

/** Edição de tempo numérica da direita para a esquerda, em décimos de segundo.
 * Exemplos: 1 -> 00:00.1, 00074 -> 00:07.4 e 01235 -> 01:23.5.
 * O campo guarda apenas dígitos: pontos/dois-pontos são desenhados pela máscara.
 */
object NumericTime {
    const val MAX_DIGITS = 9
    fun sanitize(input: String): String = input.filter(Char::isDigit).take(MAX_DIGITS)
    fun toMs(digits: String): Long? {
        val raw = sanitize(digits)
        if (raw.isEmpty()) return null
        val v = raw.padStart(5, '0')
        val tenths = v.takeLast(1).toLong()
        val seconds = v.dropLast(1).takeLast(2).toLong()
        val higher = v.dropLast(3)
        val minutes = higher.takeLast(2).toLong()
        val hours = higher.dropLast(2).ifEmpty { "0" }.toLongOrNull() ?: return null
        if (seconds >= 60 || (hours > 0 && minutes >= 60)) return null
        return runCatching {
            Math.addExact(Math.multiplyExact(
                Math.addExact(Math.multiplyExact(hours, 3600L),
                    Math.addExact(Math.multiplyExact(minutes, 60L), seconds)), 1000L),
                tenths * 100L)
        }.getOrNull()
    }
    fun fromMs(ms: Long): String {
        val tenths = ms.coerceAtLeast(0L) / 100L
        val sec = tenths / 10L
        val seconds = sec % 60L
        val minutes = if (sec < 3600L) sec / 60L else sec / 60L % 60L
        val hours = sec / 3600L
        return if (hours > 0) "%02d%02d%02d%d".format(hours, minutes, seconds, tenths % 10L)
            else "%02d%02d%d".format(minutes, seconds, tenths % 10L)
    }
    fun format(digits: String): String {
        val raw = sanitize(digits).padStart(5, '0')
        val fraction = raw.takeLast(1)
        val seconds = raw.dropLast(1).takeLast(2)
        val remainder = raw.dropLast(3)
        return if (remainder.length > 2) {
            "${remainder.dropLast(2).padStart(2, '0')}:${remainder.takeLast(2)}:$seconds.$fraction"
        } else "${remainder.padStart(2, '0')}:$seconds.$fraction"
    }
}

/** Cálculo puro para o editor de cortes; não escreve no projeto antes de aplicar. */
object TrimLogic {
    const val MIN_SEGMENT_MS = 250L
    fun valid(start: Long, end: Long, sourceDuration: Long): Boolean =
        sourceDuration >= MIN_SEGMENT_MS && start >= 0L && end <= sourceDuration &&
            end - start >= MIN_SEGMENT_MS
    fun canSplit(start: Long, end: Long, cursor: Long): Boolean =
        cursor > start + MIN_SEGMENT_MS && cursor < end - MIN_SEGMENT_MS
    fun clampCursor(cursor: Long, duration: Long): Long = cursor.coerceIn(0L, duration.coerceAtLeast(0L))
}
