package br.com.uaicortes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/**
 * Estado pequeno e restaurável dos controles de reprodução do editor.
 * O ExoPlayer é propriedade da tela (com release no onDispose), não do ViewModel.
 * A posição absoluta já pertence ao EditorWorkspaceState/rememberSaveable.
 */
internal class EditorPlaybackViewModel(private val state: SavedStateHandle) : ViewModel() {
    var speed by mutableFloatStateOf(normalize(state.get<Float>(KEY_SPEED)))
        private set

    fun selectProject(projectId: String) {
        if (state.get<String>(KEY_PROJECT) == projectId) return
        state[KEY_PROJECT] = projectId
        speed = 1f
        state[KEY_SPEED] = speed
    }

    fun updateSpeed(requested: Float) {
        speed = normalize(requested)
        state[KEY_SPEED] = speed
    }

    private fun normalize(value: Float?): Float =
        if (value != null && PreviewPlayback.SPEEDS.contains(value)) value else 1f

    companion object {
        private const val KEY_PROJECT = "editor_playback_project_id"
        private const val KEY_SPEED = "editor_playback_speed"
    }
}
