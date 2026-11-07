package br.com.uaicortes

import java.util.Locale

/** Telemetria local do download: nenhuma consulta extra à API, token ou conteúdo do APK. */
internal object GitHubDownloadProgress {
    private const val MIB = 1024.0 * 1024.0

    fun rateBytesPerSecond(bytesTransferred: Long, elapsedMs: Long): Long =
        if (bytesTransferred <= 0L || elapsedMs <= 0L) 0L
        else (bytesTransferred.toDouble() * 1000.0 / elapsedMs).toLong().coerceAtLeast(0L)

    fun readableBytes(bytes: Long): String =
        String.format(Locale.forLanguageTag("pt-BR"), "%.1f MiB", bytes.coerceAtLeast(0L) / MIB)

    fun readableRate(bytesPerSecond: Long): String =
        if (bytesPerSecond <= 0L) "Calculando velocidade…"
        else String.format(Locale.forLanguageTag("pt-BR"), "%.2f MiB/s", bytesPerSecond / MIB)

    fun remainingSeconds(received: Long, total: Long, bytesPerSecond: Long): Long? =
        if (total <= 0L || received >= total || bytesPerSecond <= 0L) null
        else ((total - received).coerceAtLeast(0L) + bytesPerSecond - 1L) / bytesPerSecond
}
