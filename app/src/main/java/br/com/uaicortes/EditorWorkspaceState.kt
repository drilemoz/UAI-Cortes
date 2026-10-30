package br.com.uaicortes

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Estado transitório compartilhado entre as ferramentas do editor.
 * Não é a persistência do projeto: alterações permanentes continuam no ProjectStore.
 * Um futuro SavedStateHandle pode restaurar IDs/rotas após morte de processo.
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
}
