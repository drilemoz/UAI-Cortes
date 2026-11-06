package br.com.uaicortes

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Ajuda contextual sob demanda; avisos essenciais de privacidade continuam visíveis. */
@Composable
internal fun SubtitleHelpButton(title: String, explanation: String) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp)) {
        Icon(Icons.Default.HelpOutline, "Ajuda: $title", modifier = Modifier.size(21.dp))
    }
    if (expanded) AlertDialog(onDismissRequest = { expanded = false },
        title = { Text(title) },
        text = { Text(explanation) },
        confirmButton = { TextButton(onClick = { expanded = false }) { Text("Entendi") } })
}
