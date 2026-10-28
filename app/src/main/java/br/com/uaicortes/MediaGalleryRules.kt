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
