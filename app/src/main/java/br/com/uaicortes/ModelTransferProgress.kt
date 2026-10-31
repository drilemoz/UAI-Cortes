package br.com.uaicortes

import java.util.Locale

/** Contadores independentes do servidor; 0% só representa menos de 1% recebido. */
internal object ModelTransferProgress {
    fun percent(bytes: Long, total: Long?): Int? =
        if (total == null || total <= 0) null
        else ((bytes.coerceAtLeast(0) * 100L / total).coerceIn(0L, 100L)).toInt()

    fun bytesLabel(value: Long): String = when {
        value < 1024L -> "$value B"
        value < 1048576L -> "${value / 1024L} KiB"
        else -> String.format(Locale.forLanguageTag("pt-BR"), "%.1f MiB", value / 1048576.0)
    }
}
