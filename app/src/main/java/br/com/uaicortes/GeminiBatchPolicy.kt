package br.com.uaicortes

/** Regras de lotes de TEXTO, nunca TTS/MP4. Sem rede, credencial ou Android. */
internal object GeminiBatchPolicy {
    const val MAX_ITEMS = 8
    const val MAX_PROMPT_LENGTH = 1000
    const val MAX_INPUT_LENGTH = 8000
    private val jobPattern = Regex("batches/[a-zA-Z0-9_-]{1,128}")

    fun prompts(raw: String): List<String> {
        require(raw.length <= MAX_INPUT_LENGTH) { "Texto acima do limite de 8.000 caracteres." }
        val items = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        require(items.size in 2..MAX_ITEMS) { "Informe de 2 a 8 pedidos, um por linha." }
        require(items.all { it.length <= MAX_PROMPT_LENGTH }) {
            "Cada linha pode ter no máximo 1.000 caracteres."
        }
        return items
    }
    fun model(id: String): String = id.trim().also {
        require(GeminiCaptionModelPolicy.validId(it)) {
            "Informe um modelo Gemini de texto compatível com Batch, sem TTS."
        }
    }
    fun jobName(name: String): String = name.trim().also {
        require(jobPattern.matches(it)) { "Nome inválido. Use o formato batches/123456." }
    }
    fun status(state: String): String = when (state) {
        "JOB_STATE_SUCCEEDED" -> "Concluído"
        "JOB_STATE_FAILED" -> "Falhou"
        "JOB_STATE_CANCELLED" -> "Cancelado"
        "JOB_STATE_EXPIRED" -> "Expirado"
        "JOB_STATE_PENDING", "JOB_STATE_QUEUED" -> "Na fila"
        "JOB_STATE_RUNNING" -> "Processando"
        "REVIEW" -> "Envio incerto: revisar"
        else -> "Aguardando consulta"
    }
}

internal data class GeminiBatchRecord(
    val localId: String, val profileId: String, val projectId: String,
    val model: String, val count: Int, val remoteName: String,
    val state: String, val createdAtMs: Long,
    val flowProjectId: String = "", val flowScenes: List<FlowBatchSceneRef> = emptyList()
)
