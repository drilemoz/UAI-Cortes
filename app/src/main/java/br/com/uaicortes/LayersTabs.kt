package br.com.uaicortes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
    enabled: Boolean = true, value: String? = null, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(58.dp), enabled = enabled,
        shape = UaiShapes.item, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            value?.let {
                Text(it, fontSize = 10.sp, lineHeight = 11.sp, maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


/** Cabeçalho da lista: o chamador mantém no máximo um grupo de controles aberto. */
@Composable
internal fun LayerOptionRow(label: String, value: String? = null, open: Boolean,
    onClick: () -> Unit) {
    val stateLabel = if (open) "Expandido" else "Recolhido"
    Surface(onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { stateDescription = stateLabel },
        shape = UaiShapes.item, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            value?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }
    }
}

/** Usa o espaço medido depois dos controles. O vídeo e as camadas mantêm a mesma proporção. */
@Composable
internal fun LayerPreviewFrame(aspect: Float, modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val fit = AdaptiveVideoLayout.fit(maxWidth.value, maxHeight.value, aspect)
        if (fit.width > 0f && fit.height > 0f) {
            Box(Modifier.size(fit.width.dp, fit.height.dp).clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF101014)), content = content)
        }
    }
}
