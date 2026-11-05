package br.com.uaicortes

/** Planejamento puro: não chama Gemini nem altera legendas, WAVs ou timeline. */
internal data class VoiceIaBatchItem(val cueId: String, val reusable: Boolean)
internal data class VoiceIaBatchPlan(val total: Int, val reusable: Int, val toGenerate: Int) {
    val savedCalls: Int get() = reusable
}
internal object VoiceIaBatchPlanner {
    fun plan(items: List<VoiceIaBatchItem>): VoiceIaBatchPlan {
        val unique = items.distinctBy { it.cueId }
        val ready = unique.count { it.reusable }
        return VoiceIaBatchPlan(unique.size, ready, unique.size - ready)
    }
}
