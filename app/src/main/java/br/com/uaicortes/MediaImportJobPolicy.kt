package br.com.uaicortes

/** Vincula uma importação assíncrona ao projeto presente quando a seleção começou. */
internal data class VideoImportRequest(val sourceProjectId: String, val createNew: Boolean) {
    fun stillTargets(currentProjectId: String): Boolean = sourceProjectId == currentProjectId
}
