package br.com.uaicortes

/** Conversão exclusivamente local de legendas existentes em rascunho de narração.
 * Não transcreve novamente, não altera as legendas originais e não chama API.
 */
internal data class NarrationSubtitleLine(
    val id: String,
    val text: String,
    val startMs: Long,
    val endMs: Long
)

internal object SubtitleNarrationReuse {
    const val MAX_CHARACTERS = 4000

    /** Respeita a ordem temporal da timeline e não inventa conteúdo para falas vazias. */
    fun eligible(lines: List<NarrationSubtitleLine>): List<NarrationSubtitleLine> = lines
        .filter { it.id.isNotBlank() && it.text.isNotBlank() && it.startMs >= 0L && it.endMs > it.startMs }
        .distinctBy { it.id }
        .sortedWith(compareBy<NarrationSubtitleLine> { it.startMs }.thenBy { it.endMs }.thenBy { it.id })

    fun assembled(lines: List<NarrationSubtitleLine>, selectedIds: Set<String>): String =
        eligible(lines).asSequence()
            .filter { it.id in selectedIds }
            .map { it.text.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }
            .joinToString("\n")

    /** A fonte é imutável durante a confirmação; nenhuma truncagem silenciosa. */
    fun canImport(text: String): Boolean = text.isNotBlank() && text.length <= MAX_CHARACTERS
}
