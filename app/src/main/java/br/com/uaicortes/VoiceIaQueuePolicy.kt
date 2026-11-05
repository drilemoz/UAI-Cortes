package br.com.uaicortes

/**
 * Registro de uma fila explícita de TTS. Nunca contém chave, prompt ou texto de legenda.
 * IN_FLIGHT deve virar REVIEW após reinício: a chamada remota pode ter consumido cota.
 */
internal enum class VoiceIaQueueStage { PENDING, IN_FLIGHT, REVIEW, READY, STALE }

internal data class VoiceIaQueueItem(
    val cueId: String,
    val cueFingerprint: String,
    val voiceId: String,
    val model: String,
    val style: String,
    val requestStyle: String,
    val stage: VoiceIaQueueStage = VoiceIaQueueStage.PENDING
)

internal data class VoiceIaQueueSnapshot(val projectId: String, val items: List<VoiceIaQueueItem>) {
    val pending: Int get() = items.count { it.stage == VoiceIaQueueStage.PENDING }
    val review: Int get() = items.count { it.stage == VoiceIaQueueStage.REVIEW }
    val ready: Int get() = items.count { it.stage == VoiceIaQueueStage.READY }
    val stale: Int get() = items.count { it.stage == VoiceIaQueueStage.STALE }
    val unfinished: Boolean get() = items.any {
        it.stage == VoiceIaQueueStage.PENDING || it.stage == VoiceIaQueueStage.IN_FLIGHT ||
            it.stage == VoiceIaQueueStage.REVIEW || it.stage == VoiceIaQueueStage.STALE
    }
}

/** Operações puras, sem rede ou acesso a preferências. */
internal object VoiceIaQueuePolicy {
    const val MAX_ITEMS = 500

    fun create(projectId: String, entries: List<VoiceIaQueueItem>): VoiceIaQueueSnapshot {
        require(projectId.isNotBlank() && projectId.length <= 160)
        require(entries.isNotEmpty() && entries.size <= MAX_ITEMS)
        require(entries.map { it.cueId }.toSet().size == entries.size) { "IDs repetidos na fila de voz" }
        require(entries.all { valid(it) && it.stage == VoiceIaQueueStage.PENDING })
        return VoiceIaQueueSnapshot(projectId, entries.toList())
    }

    fun valid(item: VoiceIaQueueItem): Boolean =
        item.cueId.length in 1..160 &&
            Regex("[0-9a-f]{64}").matches(item.cueFingerprint) &&
            item.voiceId.length in 1..160 && item.model.length in 1..100 &&
            item.style.length in 1..1024 && item.requestStyle.length in 1..1600

    fun transition(queue: VoiceIaQueueSnapshot, id: String,
        to: VoiceIaQueueStage): VoiceIaQueueSnapshot {
        val old = queue.items.firstOrNull { it.cueId == id } ?: error("Fala ausente da fila")
        val allowed = when (old.stage) {
            VoiceIaQueueStage.PENDING -> to == VoiceIaQueueStage.IN_FLIGHT || to == VoiceIaQueueStage.STALE
            VoiceIaQueueStage.IN_FLIGHT -> to == VoiceIaQueueStage.READY || to == VoiceIaQueueStage.REVIEW ||
                to == VoiceIaQueueStage.PENDING
            VoiceIaQueueStage.REVIEW -> to == VoiceIaQueueStage.PENDING || to == VoiceIaQueueStage.READY ||
                to == VoiceIaQueueStage.STALE
            VoiceIaQueueStage.READY -> to == VoiceIaQueueStage.REVIEW || to == VoiceIaQueueStage.STALE
            VoiceIaQueueStage.STALE -> false
        }
        require(allowed) { "Transição de fila inválida: ${old.stage} → $to" }
        return queue.copy(items = queue.items.map { if (it.cueId == id) it.copy(stage = to) else it })
    }

    /**
     * Reconciliar somente com WAV durável verificado. Uma chamada em voo sem WAV
     * torna-se REVIEW e nunca é repetida pela retomada automática.
     */
    fun recover(queue: VoiceIaQueueSnapshot, currentFingerprints: Map<String, String>,
        ready: (VoiceIaQueueItem) -> Boolean): VoiceIaQueueSnapshot = queue.copy(items = queue.items.map { item ->
        val stage = when {
            currentFingerprints[item.cueId] != item.cueFingerprint -> VoiceIaQueueStage.STALE
            ready(item) -> VoiceIaQueueStage.READY
            item.stage == VoiceIaQueueStage.IN_FLIGHT || item.stage == VoiceIaQueueStage.READY -> VoiceIaQueueStage.REVIEW
            else -> item.stage
        }
        item.copy(stage = stage)
    })

    fun retryReviewed(queue: VoiceIaQueueSnapshot): VoiceIaQueueSnapshot = queue.copy(
        items = queue.items.map { if (it.stage == VoiceIaQueueStage.REVIEW)
            it.copy(stage = VoiceIaQueueStage.PENDING) else it }
    )
}
