package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Abas da ferramenta Camadas: PiP e Marca d'água compartilham o mesmo atalho na barra do editor. */
@Composable
internal fun LayersTabs(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((key, label) in listOf("pip" to "PiP", "watermark" to "Marca d'água")) {
            if (key == selected) Button(onClick = {}, modifier = Modifier.weight(1f)) { Text(label) }
            else OutlinedButton(onClick = { onSelect(key) }, modifier = Modifier.weight(1f)) { Text(label) }
        }
    }
}
