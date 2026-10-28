package br.com.uaicortes

/** Estado da exportação, separado da tela para manter progresso e cancelamento consistentes. */
internal enum class ExportPhase { PREPARING, RENDERING, SAVING }

internal object ExportProgressUi {
    /** Um número só é apresentado quando o Transformer realmente o disponibiliza. */
    fun renderPercent(phase: ExportPhase, percent: Int?): Int? =
        if (phase == ExportPhase.RENDERING) percent?.coerceIn(0, 100) else null

    fun canCancel(phase: ExportPhase): Boolean = phase != ExportPhase.SAVING

    fun stageLabel(phase: ExportPhase): String = when (phase) {
        ExportPhase.PREPARING -> "Preparando arquivos para exportação…"
        ExportPhase.RENDERING -> "Processando vídeo, áudio e efeitos…"
        ExportPhase.SAVING -> "Finalizando e salvando na galeria…"
    }

    fun elapsedLabel(elapsedSeconds: Long): String {
        val safe = elapsedSeconds.coerceAtLeast(0L)
        return "%02d:%02d".format(safe / 60L, safe % 60L)
    }
}
