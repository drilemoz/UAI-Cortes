package br.com.uaicortes

import java.util.Locale

/** Somente apresentação: não arredonda nem desloca horários de legenda ou do WAV. */
internal object VoiceIaDisplayPolicy {
    fun overrunMs(startMs: Long, endMs: Long, audioDurationMs: Long): Long =
        (audioDurationMs - (endMs - startMs)).coerceAtLeast(0L)

    /** Mostra décimos arredondados para cima, evitando esconder pequenas ultrapassagens. */
    fun seconds(ms: Long): String = String.format(Locale.ROOT, "%.1f", ((ms.coerceAtLeast(0L) + 99L) / 100L) / 10.0)
}
