package br.com.uaicortes

/**
 * Estado puro da exportação. Cada tentativa recebe um identificador monotônico:
 * callbacks de exportações canceladas/antigas nunca podem concluir a tentativa atual.
 * Este objeto não conhece Android, arquivos, MediaStore ou Compose.
 */
internal data class ExportTaskSnapshot(
    val id: Long = 0L,
    val working: Boolean = false,
    val phase: ExportPhase = ExportPhase.PREPARING,
    val progress: Int? = null,
    val elapsedSeconds: Long = 0L,
    val startedAtMs: Long = 0L,
    val fileName: String = "",
    val quality: Int = 1080,
    val message: String = ""
)

internal class ExportTaskCoordinator {
    private var lastId = 0L
    var snapshot: ExportTaskSnapshot = ExportTaskSnapshot()
        private set

    fun start(fileName: String, quality: Int, nowMs: Long): Long? {
        if (snapshot.working) return null
        require(fileName.isNotBlank()) { "Informe um nome para o vídeo." }
        require(quality == 720 || quality == 1080) { "Resolução inválida." }
        lastId += 1
        snapshot = ExportTaskSnapshot(id = lastId, working = true,
            startedAtMs = nowMs.coerceAtLeast(0L), fileName = fileName, quality = quality)
        return lastId
    }

    fun rendering(id: Long): Boolean = changeIfActive(id, ExportPhase.PREPARING) {
        it.copy(phase = ExportPhase.RENDERING)
    }

    fun tick(id: Long, percent: Int?, nowMs: Long): Boolean = changeIfActive(id) {
        it.copy(elapsedSeconds = ((nowMs - it.startedAtMs).coerceAtLeast(0L) / 1000L),
            progress = ExportProgressUi.renderPercent(it.phase, percent))
    }

    fun saving(id: Long): Boolean = changeIfActive(id) {
        it.copy(phase = ExportPhase.SAVING, progress = null)
    }

    fun success(id: Long): Boolean = changeIfActive(id) {
        it.copy(working = false, progress = null, message = "")
    }

    fun failure(id: Long, reason: String): Boolean = changeIfActive(id) {
        it.copy(working = false, progress = null, message = "Erro: $reason")
    }

    fun cancel(id: Long): Boolean {
        if (snapshot.phase == ExportPhase.SAVING) return false
        return changeIfActive(id) {
            it.copy(working = false, progress = null, message = "Exportação cancelada.")
        }
    }

    /** Descarta apenas resultados finais, sem afetar uma exportação em andamento. */
    fun reset(): Boolean {
        if (snapshot.working) return false
        snapshot = ExportTaskSnapshot(id = snapshot.id)
        return true
    }

    private fun changeIfActive(
        id: Long,
        onlyPhase: ExportPhase? = null,
        update: (ExportTaskSnapshot) -> ExportTaskSnapshot
    ): Boolean {
        val current = snapshot
        if (!current.working || current.id != id ||
            (onlyPhase != null && current.phase != onlyPhase)) return false
        snapshot = update(current)
        return true
    }
}
