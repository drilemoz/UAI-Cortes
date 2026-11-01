package br.com.uaicortes

/** Desfaz somente o lote do último snapshot, sem sobrescrever edições posteriores. */
internal data class SubtitleBatchUndo(val previous:List<SubtitleCue>, val updated:List<SubtitleCue>) {
    fun restoreIfCurrent(current:List<SubtitleCue>):List<SubtitleCue>? =
        previous.takeIf { current==updated }
}
