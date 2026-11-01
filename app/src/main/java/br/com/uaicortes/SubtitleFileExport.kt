package br.com.uaicortes

/** Exporta uma cópia textual sem modificar o projeto nem os tempos originais. */
internal object SubtitleFileExport {
    enum class Format { SRT, TXT }
    private fun time(ms: Long): String {
        val safe = ms.coerceAtLeast(0)
        val hours = safe / 3_600_000
        val minutes = (safe / 60_000) % 60
        val seconds = (safe / 1_000) % 60
        val millis = safe % 1_000
        return "%02d:%02d:%02d,%03d".format(java.util.Locale.ROOT, hours, minutes, seconds, millis)
    }

    fun render(cues: List<SubtitleCue>, format: Format): String {
        require(cues.size <= 20_000) { "Muitas legendas para exportar." }
        val sorted = cues.filter { it.text.isNotBlank() && it.endMs > it.startMs }
            .sortedWith(compareBy<SubtitleCue> { it.startMs }.thenBy { it.id })
        val content = buildString {
            sorted.forEachIndexed { index, cue ->
                val plain = cue.text.replace("\r\n", "\n").replace('\r', '\n').trim()
                if (format == Format.SRT) {
                    append(index + 1).append('\n')
                    append(time(cue.startMs)).append(" --> ").append(time(cue.endMs)).append('\n')
                    append(plain).append("\n\n")
                } else append(plain).append('\n')
            }
        }
        require(content.toByteArray(Charsets.UTF_8).size <= 2_000_000) { "Arquivo de legendas muito grande." }
        return content
    }
}
