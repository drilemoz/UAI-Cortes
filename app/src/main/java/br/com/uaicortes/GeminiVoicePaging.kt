package br.com.uaicortes

/** Paginação apenas de exibição: pesquisa, ordenação e filtros percorrem o catálogo local todo. */
internal object GeminiVoicePaging {
    const val PAGE_SIZE = 50

    fun totalPages(total: Int): Int = if (total <= 0) 0 else 1 + (total - 1) / PAGE_SIZE

    fun range(total: Int, requestedPage: Int): Pair<Int, Int> {
        if (total <= 0) return 0 to 0
        val page = requestedPage.coerceIn(0, totalPages(total) - 1)
        val from = page * PAGE_SIZE
        return (from + 1) to (from + PAGE_SIZE).coerceAtMost(total)
    }

    fun <T> slice(source: List<T>, requestedPage: Int): List<T> {
        if (source.isEmpty()) return emptyList()
        val page = requestedPage.coerceIn(0, totalPages(source.size) - 1)
        val from = page * PAGE_SIZE
        return source.subList(from, (from + PAGE_SIZE).coerceAtMost(source.size))
    }
}
