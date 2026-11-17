package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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


@Composable
internal fun LayerTool(label: String, icon: ImageVector, modifier: Modifier = Modifier,
    enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(58.dp), enabled = enabled,
        shape = UaiShapes.item, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
}
