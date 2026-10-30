package br.com.uaicortes

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue

/**
 * Estado de navegação/seleção, separado das edições persistidas no ProjectStore.
 * Somente identificadores e posição são colocados no SavedStateRegistry, nunca mídia/JSON.
 * O Android pode não fornecer savedInstanceState (ex.: encerramento voluntário do app).
 */
@Stable
internal class EditorWorkspaceState {
    var screen by mutableStateOf("home")
    var previous by mutableStateOf("home")
    var selectedClip by mutableStateOf("")
    var selectedAudioTrackId by mutableStateOf<String?>(null)
    var selectedSubtitleId by mutableStateOf<String?>(null)
    var selectedOverlayId by mutableStateOf("")
    var selectedBoundary by mutableIntStateOf(0)
    var positionMs by mutableLongStateOf(0L)

    fun reconcile(project: VideoProject?, mediaOrigin: String = "home", mediaAction: String = "new") {
        screen = EditorNavigation.restoreRoute(screen, project != null, mediaOrigin, mediaAction)
        if (project == null) {
            selectedClip = ""
            selectedAudioTrackId = null
            selectedSubtitleId = null
            selectedOverlayId = ""
            selectedBoundary = 0
            positionMs = 0L
            return
        }
        if (selectedClip !in project.clips.map { it.id })
            selectedClip = TrimTimelineEdit.resumeClipId(project)
        if (project.audioTracks.none { it.id == selectedAudioTrackId }) selectedAudioTrackId = null
        if (project.subtitles.none { it.id == selectedSubtitleId }) selectedSubtitleId = null
        if (project.overlays.none { it.id == selectedOverlayId }) selectedOverlayId = project.overlays.firstOrNull()?.id.orEmpty()
        selectedBoundary = selectedBoundary.coerceIn(0, (project.clips.size - 2).coerceAtLeast(0))
        positionMs = positionMs.coerceIn(0L, project.durationMs.coerceAtLeast(0L))
    }

    /** Versão interna do payload, independente da versão dos projetos de vídeo. */
    internal fun snapshotTokens(): List<String> = listOf(
        "1", screen, previous, selectedClip, selectedAudioTrackId.orEmpty(),
        selectedSubtitleId.orEmpty(), selectedOverlayId, selectedBoundary.toString(), positionMs.toString()
    )

    companion object {
        fun fromTokens(values: List<String>): EditorWorkspaceState = EditorWorkspaceState().apply {
            if (values.size != 9 || values[0] != "1") return@apply
            screen = EditorNavigation.knownRoute(values[1])
            previous = EditorNavigation.knownRoute(values[2])
            selectedClip = values[3]
            selectedAudioTrackId = values[4].ifBlank { null }
            selectedSubtitleId = values[5].ifBlank { null }
            selectedOverlayId = values[6]
            selectedBoundary = values[7].toIntOrNull()?.coerceAtLeast(0) ?: 0
            positionMs = values[8].toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        }

        val Saver: Saver<EditorWorkspaceState, ArrayList<String>> = Saver(
            save = { ArrayList(it.snapshotTokens()) },
            restore = { fromTokens(it) }
        )
    }
}
