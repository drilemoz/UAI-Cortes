package br.com.uaicortes

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test

class EditorPlaybackViewModelTest {
    @Test fun defaultSpeedAndResetOnDifferentProject() {
        val state = SavedStateHandle()
        val vm = EditorPlaybackViewModel(state)
        assertEquals(1f, vm.speed, 0.001f)
        vm.selectProject("first")
        vm.updateSpeed(1.5f)
        vm.selectProject("first")
        assertEquals(1.5f, vm.speed, 0.001f)
        vm.selectProject("second")
        assertEquals(1f, vm.speed, 0.001f)
    }

    @Test fun rejectsUnsupportedSpeedAndRestoresOnlyScalars() {
        val state = SavedStateHandle(mapOf("editor_playback_project_id" to "first", "editor_playback_speed" to 2f))
        val vm = EditorPlaybackViewModel(state)
        assertEquals(2f, vm.speed, 0.001f)
        vm.updateSpeed(Float.NaN)
        assertEquals(1f, vm.speed, 0.001f)
        assertEquals("first", state.get<String>("editor_playback_project_id"))
    }
}
