package br.com.uaicortes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Somente edição ativa: o Android não garante temporizadores com o app encerrado. */
@Composable
internal fun ProjectBackupLifecycle(backups: ProjectBackupStore, activeProjectId: String, screen: String) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(backups, activeProjectId, screen) {
        if (activeProjectId.isBlank() || screen in setOf("home", "projects", "settings", "general",
                "backup_settings", "appearance", "about", "whats_new", "diagnostics", "speech_model",
                "gemini_settings")) return@LaunchedEffect
        withContext(Dispatchers.IO) { runCatching { backups.snapshot(activeProjectId) } }
        while (true) {
            delay(backups.preferences.intervalSeconds * 1000L)
            if (backups.preferences.enabled) withContext(Dispatchers.IO) {
                runCatching { backups.snapshot(activeProjectId) }
            }
        }
    }
    // Também registra ao deixar o editor, mesmo antes de completar o intervalo escolhido.
    DisposableEffect(backups, activeProjectId, screen) {
        onDispose {
            if (activeProjectId.isNotBlank() && screen !in setOf("home", "projects", "settings",
                    "general", "backup_settings", "appearance", "about", "whats_new", "diagnostics") &&
                backups.preferences.enabled) {
                scope.launch(Dispatchers.IO) { runCatching { backups.snapshot(activeProjectId) } }
            }
        }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, backups, activeProjectId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && activeProjectId.isNotBlank() &&
                backups.preferences.enabled) {
                scope.launch(Dispatchers.IO) { runCatching { backups.snapshot(activeProjectId) } }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
