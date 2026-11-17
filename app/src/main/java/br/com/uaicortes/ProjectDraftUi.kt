package br.com.uaicortes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

/** Decide what to do before leaving without touching the saved original. */
@Composable
internal fun ConfirmEditorExit(hasSavedOriginal: Boolean, projectTitle: String,
    onDecision: (String) -> Unit, onNamedDraft: (String) -> Boolean, onContinue: () -> Unit) {
    var naming by remember { mutableStateOf(false) }
    var draftName by remember(projectTitle) { mutableStateOf("${projectTitle.take(69)} (rascunho)") }
    var saveFailed by remember { mutableStateOf(false) }
    if (naming) {
        AlertDialog(onDismissRequest = { naming = false }, shape = UaiShapes.dialog,
            title = { Text("Nome do rascunho") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Uma versão separada em Projetos. O original não será substituído.")
                    OutlinedTextField(value = draftName, onValueChange = {
                        draftName = it.take(80); saveFailed = false
                    }, label = { Text("Nome do projeto") }, singleLine = true)
                    if (saveFailed) Text("Não foi possível guardar o rascunho.",
                        color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = { saveFailed = !onNamedDraft(ProjectNaming.title(draftName)) },
                    enabled = ProjectNaming.validTitle(draftName)) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { naming = false }) { Text("Voltar") } })
        return
    }
    AlertDialog(
        onDismissRequest = onContinue, shape = UaiShapes.dialog,
        icon = { Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp)) },
        title = { Text("Salvar edição?") },
        text = { Text(if (hasSavedOriginal) "Deseja guardar as alterações antes de sair?"
            else "Guardar este projeto antes de sair? Os arquivos originais permanecem no celular.") },
        confirmButton = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CompactActionTile("Salvar", Icons.Default.Save, primary = true) { onDecision("save") }
                CompactActionTile("Rascunho", Icons.Default.ContentCopy) { naming = true }
                CompactActionTile("Descartar", Icons.Default.DeleteOutline, danger = true) { onDecision("discard") }
                CompactActionTile("Editar", Icons.Default.Edit) { onContinue() }
            }
        }
    )
}

@Composable
internal fun ConfirmRecoverDraft(onRecover: () -> Unit, onDiscard: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(onDismissRequest = {}, shape = UaiShapes.dialog,
        icon = {
            Surface(color = colors.primaryContainer, shape = UaiShapes.card) {
                Icon(Icons.Default.History, contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.padding(10.dp).size(22.dp))
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Recuperar edição interrompida?", style = MaterialTheme.typography.headlineSmall)
                Text("Rascunho temporário encontrado", style = MaterialTheme.typography.labelLarge,
                    color = colors.primary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Há uma sessão temporária que não foi salva como projeto. Deseja continuar exatamente de onde parou?")
                Surface(color = colors.surfaceVariant.copy(alpha = 0.75f), shape = UaiShapes.card) {
                    Text("Os arquivos originais não foram copiados nem substituídos.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDiscard, shape = UaiShapes.button, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                    Text("Descartar rascunho", textAlign = TextAlign.Center)
                }
                Button(onClick = onRecover, shape = UaiShapes.button, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                    Text("Recuperar edição", textAlign = TextAlign.Center)
                }
            }
        })
}

@Composable
internal fun InvalidEditorDraft(onClear: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(onDismissRequest = {}, shape = UaiShapes.dialog,
        icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = colors.error) },
        title = { Text("Rascunho não recuperável") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("A sessão temporária está incompleta ou o projeto original foi modificado.")
                Surface(color = colors.errorContainer, shape = UaiShapes.card) {
                    Text("Nenhum projeto salvo será substituído.", modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onErrorContainer)
                }
            }
        },
        confirmButton = { Button(onClick = onClear, shape = UaiShapes.button) { Text("Limpar rascunho inválido") } })
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
