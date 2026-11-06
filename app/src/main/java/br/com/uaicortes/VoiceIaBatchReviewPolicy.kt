package br.com.uaicortes

/** Diagnóstico local. Categorias de revisão podem se sobrepor. */
internal data class VoiceIaReviewItem(
    val cueId: String, val speakerId: String?, val hasVoice: Boolean,
    val wavReady: Boolean, val outdated: Boolean, val syncIssue: Boolean,
    val uncertain: Boolean
)
internal data class VoiceIaReviewSummary(
    val total: Int, val ready: Int, val pending: Int, val missingVoice: Int,
    val outdated: Int, val syncReview: Int, val uncertain: Int
)
internal object VoiceIaBatchReviewPolicy {
    fun review(items: List<VoiceIaReviewItem>): VoiceIaReviewSummary {
        val unique = items.distinctBy { it.cueId }
        val ready = unique.count { it.wavReady }
        return VoiceIaReviewSummary(unique.size, ready,
            unique.count { it.hasVoice && !it.wavReady }, unique.count { !it.hasVoice },
            unique.count { it.outdated }, unique.count { it.syncIssue },
            unique.count { it.uncertain })
    }
}
