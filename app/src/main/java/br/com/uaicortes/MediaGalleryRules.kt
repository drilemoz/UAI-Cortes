package br.com.uaicortes

internal enum class GalleryTab { RECENT, VIDEOS, IMAGES }

/** Regras puras: busca não toca no texto salvo, no caminho ou na mídia original. */
internal object GalleryRules {
    fun matches(name: String, folder: String, search: String): Boolean {
        val query = search.trim()
        return query.isBlank() || name.contains(query, ignoreCase = true) ||
            folder.contains(query, ignoreCase = true)
    }

    fun accepts(tab: GalleryTab, isImage: Boolean): Boolean = when (tab) {
        GalleryTab.RECENT -> true
        GalleryTab.VIDEOS -> !isImage
        GalleryTab.IMAGES -> isImage
    }
}

/** Somente falhas reais devem aparecer na galeria; cancelamentos são tratados pelo ciclo de vida. */
internal object GalleryLoadErrors {
    fun message(error: Throwable): String = when (error) {
        is SecurityException -> "Acesso à mídia não autorizado. Revise as permissões ou escolha pelo Android."
        else -> "Não foi possível carregar a galeria. Tente novamente ou escolha pelo Android."
    }
}
