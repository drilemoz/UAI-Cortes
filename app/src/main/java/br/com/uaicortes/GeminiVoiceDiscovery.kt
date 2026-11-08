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

    /** Seleciona duas prévias já disponíveis, dando prioridade a pares ainda não avaliados.
     * A ordem é fornecida pela UI (embaralhada somente quando o usuário pede), sem chamadas de rede.
     */
    fun nextOfflinePair(orderedIds: List<String>, offlineIds: Set<String>,
                        reviews: Map<String, String>, previous: String? = null): Pair<String, String>? {
        val ids = orderedIds.asSequence().filter { it in offlineIds && GeminiVoiceFavorites.valid(it) }
            .distinct().take(GeminiVoiceLibrary.MAX_VOICES).toList()
        if (ids.size < 2) return null
        var fallback: Pair<String, String>? = null
        for (i in 0 until ids.lastIndex) for (j in i + 1 until ids.size) {
            val key = pairKey(ids[i], ids[j]) ?: continue
            val pair = ids[i] to ids[j]
            if (key != previous) {
                if (key !in reviews) return pair
                if (fallback == null) fallback = pair
            }
        }
        return fallback ?: (ids[0] to ids[1])
    }

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
        // Uma avaliação "diferentes" impede unir grupos que a contradigam,
        // inclusive quando a contradição surge por transitividade (A~B, B~C, A!=C).
        val distinctPairs = reviews.asSequence().filter { it.value == DIFFERENT }.mapNotNull { (key, _) ->
            key.split('\u001f').takeIf { it.size == 2 && it.all(available::contains) }
        }.toList()
        val members = available.associateWith { mutableSetOf(it) }.toMutableMap()
        reviews.asSequence().filter { it.value == SIMILAR }.sortedBy { it.key }.forEach { (key, _) ->
            val pair = key.split('\u001f')
            if (pair.size != 2 || pair[0] !in available || pair[1] !in available) return@forEach
            val a = root(pair[0]); val b = root(pair[1])
            if (a == b) return@forEach
            val left = members[a].orEmpty(); val right = members[b].orEmpty()
            val conflicts = distinctPairs.any { (first, second) ->
                (first in left && second in right) || (second in left && first in right)
            }
            if (!conflicts) {
                parent[b] = a
                members[a] = (left + right).toMutableSet()
                members.remove(b)
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

