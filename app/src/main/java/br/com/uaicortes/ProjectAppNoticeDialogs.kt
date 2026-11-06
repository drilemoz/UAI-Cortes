package br.com.uaicortes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CatalogStorageWarning(warning: String, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Atenção ao armazenamento de projetos") },
        text = { Text(warning) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendi") } })
}

@Composable
internal fun ActiveMediaImportDialog(onCancel: () -> Unit) {
    AlertDialog(onDismissRequest = {},
        title = { Text("Importando mídia") },
        text = { Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
            Text("Lendo duração e informações dos arquivos locais…")
        } },
        confirmButton = { TextButton(onClick = onCancel) { Text("Cancelar importação") } })
}
