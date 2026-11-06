package br.com.uaicortes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
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

/** UI da decisão de saída, sem disparar gravação permanente por conta própria. */
@Composable
internal fun ConfirmEditorExit(hasSavedOriginal: Boolean, onDecision: (String) -> Unit,
    onContinue: () -> Unit) {
    AlertDialog(onDismissRequest = onContinue,
        icon = { Icon(Icons.Default.Save, null) },
        title = { Text("Salvar este projeto?") },
        text = { Text(if (hasSavedOriginal)
            "Você alterou um projeto existente. Salve, guarde uma cópia independente ou descarte as mudanças desta sessão."
            else "Este vídeo ainda não está em Projetos. Salvar mantém o projeto; descartar sai sem criar uma cópia permanente. Os arquivos originais não serão apagados.") },
        confirmButton = { Column {
            TextButton(onClick = { onDecision("save") }) { Text("Salvar projeto") }
            if (hasSavedOriginal)
                TextButton(onClick = { onDecision("copy") }) { Text("Salvar cópia") }
        } },
        dismissButton = { Column {
            TextButton(onClick = { onDecision("discard") }) {
                Text("Descartar e sair", color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = onContinue) { Text("Continuar editando") }
        } })
}

@Composable
internal fun ConfirmRecoverDraft(onRecover: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(onDismissRequest = {},
        title = { Text("Recuperar edição interrompida?") },
        text = { Text("Há uma sessão temporária que não foi salva como projeto. Deseja continuar de onde parou? Os arquivos originais não foram copiados.") },
        confirmButton = { TextButton(onClick = onRecover) { Text("Recuperar edição") } },
        dismissButton = { TextButton(onClick = onDiscard) { Text("Descartar rascunho") } })
}

@Composable
internal fun InvalidEditorDraft(onClear: () -> Unit) {
    AlertDialog(onDismissRequest = {},
        title = { Text("Rascunho não recuperável") },
        text = { Text("A sessão temporária está incompleta ou o projeto original foi modificado. Nenhum projeto salvo será substituído.") },
        confirmButton = { TextButton(onClick = onClear) { Text("Limpar rascunho inválido") } })
}

/** Grava rascunho só em área privada; não toca no ProjectStore nem no histórico. */
@Composable
internal fun ProjectDraftRecoveryLifecycle(recovery: ProjectDraftRecoveryStore,
    project: VideoProject?, baseline: VideoProject?, pending: Boolean) {
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(recovery, project, baseline, pending) {
        val snapshot = project?.takeIf { pending } ?: return@LaunchedEffect
        delay(650)
        val version = recovery.requestVersion()
        withContext(Dispatchers.IO) {
            runCatching { recovery.saveIfCurrent(version, snapshot, baseline) }
                .onFailure { DiagnosticRecorder.exception("draft_recovery_save", it) }
        }
    }
    DisposableEffect(owner, recovery, project, baseline, pending) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && pending && project != null) {
                val version = recovery.requestVersion()
                scope.launch(Dispatchers.IO) {
                    runCatching { recovery.saveIfCurrent(version, project, baseline) }
                        .onFailure { DiagnosticRecorder.exception("draft_recovery_save", it) }
                }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
