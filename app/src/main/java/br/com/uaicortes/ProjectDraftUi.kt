package br.com.uaicortes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

/** Três ações igualmente tocáveis sem empilhar botões em telas estreitas. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.EditorExitAction(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    danger: Boolean = false, action: () -> Unit
) {
    OutlinedButton(onClick = action,
        modifier = Modifier.weight(1f).height(64.dp),
        shape = UaiShapes.button,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp),
                tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** UI da decisão de saída, sem disparar gravação permanente por conta própria. */
@Composable
internal fun ConfirmEditorExit(hasSavedOriginal: Boolean, onDecision: (String) -> Unit,
    onContinue: () -> Unit) {
    AlertDialog(
        onDismissRequest = onContinue,
        shape = UaiShapes.dialog,
        icon = { Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp)) },
        title = { Text("Salvar edição?") },
        text = { Text(if (hasSavedOriginal)
            "Deseja guardar as alterações antes de sair?"
        else "Guardar este projeto antes de sair? Os arquivos originais permanecem no celular.") },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EditorExitAction("Salvar", Icons.Default.Save) { onDecision("save") }
                    EditorExitAction("Descartar", Icons.Default.DeleteOutline, danger = true) {
                        onDecision("discard")
                    }
                    EditorExitAction("Continuar", Icons.Default.Edit) { onContinue() }
                }
                if (hasSavedOriginal) {
                    TextButton(onClick = { onDecision("copy") },
                        modifier = Modifier.align(Alignment.End)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp))
                        Text("Salvar como cópia")
                    }
                }
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
        confirmButton = { Button(onClick = onRecover, shape = UaiShapes.button) { Text("Recuperar edição") } },
        dismissButton = { TextButton(onClick = onDiscard, shape = UaiShapes.button) { Text("Descartar rascunho") } })
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
