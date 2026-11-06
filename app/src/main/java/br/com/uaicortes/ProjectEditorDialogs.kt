package br.com.uaicortes

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
internal fun ConfirmDeleteProjectDialog(title: String, onDelete: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(onDismissRequest = onCancel, icon = { Icon(Icons.Default.DeleteOutline, null) },
        title = { Text("Excluir projeto?") },
        text = { Text("O projeto \"$title\" e suas edições salvas serão removidos. Os vídeos e fotos originais não serão apagados. Esta ação não pode ser desfeita.") },
        confirmButton = { TextButton(onClick = onDelete) {
            Text("Excluir projeto", color = MaterialTheme.colorScheme.error)
        } }, dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } })
}

@Composable
internal fun ConfirmSequentialOverlay(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(onDismissRequest = onCancel, icon = { Icon(Icons.Default.Schedule, null) },
        title = { Text("O PiP anterior já chega ao final") },
        text = { Text("Não há tempo disponível depois do último PiP. Quer adicionar o novo no mesmo intervalo do anterior? A posição será conferida para evitar que cubra outro PiP.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Adicionar no mesmo período") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } })
}

@Composable
internal fun ConfirmOverlayConflict(onKeep: () -> Unit, onRearrange: () -> Unit,
    onCancel: () -> Unit) {
    AlertDialog(onDismissRequest = onCancel, icon = { Icon(Icons.Default.Layers, null) },
        title = { Text("PiPs sobrepostos") },
        text = { Text("Os PiPs ocupam a mesma região durante parte do vídeo. Você pode reorganizá-los automaticamente ou confirmar que deseja manter um PiP sobre o outro.") },
        confirmButton = { Column {
            TextButton(onClick = onKeep) { Text("Manter sobreposição") }
            TextButton(onClick = onRearrange) { Text("Reposicionar automaticamente") }
        } }, dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } })
}
