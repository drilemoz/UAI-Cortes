package br.com.uaicortes

/** Comparações são opiniões locais do usuário, nunca medições acústicas ou conclusões da API. */
internal object GeminiVoiceDiscovery {
    const val SIMILAR = "similar"
    const val DIFFERENT = "different"
    const val MAX_REVIEWS = 400

    fun pairKey(a: String, b: String): String? {
        if (!GeminiVoiceFavorites.valid(a) || !GeminiVoiceFavorites.valid(b) || a == b) return null
        return listOf(a, b).sorted().joinToString("\u001f")
    }

    fun merge(existing: List<GeminiVoice>, fetched: List<GeminiVoice>, limit: Int = GeminiVoiceLibrary.MAX_VOICES): List<GeminiVoice> {
        // Retém vozes já armazenadas, favoritos e prévias vinculadas a IDs antigos.
        if (limit <= 0) return emptyList()
        val result = LinkedHashMap<String, GeminiVoice>()
        existing.asSequence().filter { GeminiVoiceFavorites.valid(it.id) }.forEach {
            if (result.size < limit) result[it.id] = it
        }
        fetched.asSequence().filter { GeminiVoiceFavorites.valid(it.id) }.forEach {
            if (it.id in result) result[it.id] = it
            else if (result.size < limit) result[it.id] = it
        }
        return result.values.toList()
    }

    fun isPortuguese(voice: GeminiVoice): Boolean = voice.languageCode.equals("pt-BR", true) ||
        voice.languageCode.equals("pt", true)

    /** Oculta apenas entradas marcadas explicitamente como parecidas; uma voz de cada grupo permanece. */
    fun hiddenFromReview(voices: List<GeminiVoice>, reviews: Map<String, String>,
                         favoriteIds: Set<String>, savedIds: Set<String>): Set<String> {
        val available = voices.map { it.id }.toSet()
        val parent = available.associateWith { it }.toMutableMap()
        fun root(id: String): String {
            var current = id
            while (parent[current] != current) current = parent[current] ?: current
            return current
        }
        reviews.forEach { (key, value) ->
            if (value == SIMILAR) {
                val pair = key.split('\u001f')
                if (pair.size == 2 && pair[0] in available && pair[1] in available) {
                    val a = root(pair[0]); val b = root(pair[1])
                    if (a != b) parent[b] = a
                }
            }
        }
        return voices.groupBy { root(it.id) }.values.flatMap { group ->
            if (group.size <= 1) emptyList() else {
                val keeper = group.minWithOrNull(compareByDescending<GeminiVoice> { it.id in favoriteIds }
                    .thenByDescending { it.id in savedIds }.thenBy { it.id })!!
                group.map { it.id }.filter { it != keeper.id }
            }
        }.toSet()
    }
}

