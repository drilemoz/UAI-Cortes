package br.com.uaicortes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SubtitleExportDialog(onClose: () -> Unit, canSaveDownloads: Boolean,
    onChooseDestination: (SubtitleFileExport.Format) -> Unit, onSaveSrtToDownloads: () -> Unit) {
    var format by remember { mutableStateOf(SubtitleFileExport.Format.SRT) }
    var help by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onClose, shape = UaiShapes.dialog,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Exportar legendas", Modifier.weight(1f))
                IconButton(onClick = { help = !help }) { Icon(Icons.Default.Info, "Sobre os formatos") }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Os dois formatos mantêm os horários.")
                if (help) Text("SRT e TXT incluem o início e o fim com milissegundos e podem ser reimportados. " +
                    "Cores e animações não fazem parte do arquivo.",
                    style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = format == SubtitleFileExport.Format.SRT,
                        onClick = { format = SubtitleFileExport.Format.SRT },
                        label = { Text("SRT") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = format == SubtitleFileExport.Format.TXT,
                        onClick = { format = SubtitleFileExport.Format.TXT },
                        label = { Text("TXT") }, modifier = Modifier.weight(1f))
                }
                if (canSaveDownloads && format == SubtitleFileExport.Format.SRT)
                    OutlinedButton(onClick = onSaveSrtToDownloads,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                        Text("SRT em Downloads")
                    }
            }
        },
        confirmButton = {
            Button(onClick = { onChooseDestination(format) }) { Text("Escolher destino") }
        },
        dismissButton = { OutlinedButton(onClick = onClose) { Text("Fechar") } })
}
