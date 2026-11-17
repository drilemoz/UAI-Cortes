package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Uniform, touch-friendly controls for a dense row. */
@Composable
internal fun RowScope.CompactActionTile(label: String, icon: ImageVector,
    primary: Boolean = false, danger: Boolean = false, enabled: Boolean = true,
    onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val content: @Composable RowScope.() -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
                tint = if (danger && enabled) colors.error else LocalContentColor.current)
            Text(label, fontSize = 11.sp, lineHeight = 12.sp, maxLines = 2,
                textAlign = TextAlign.Center,
                color = if (danger && enabled) colors.error else LocalContentColor.current)
        }
    }
    val tile = Modifier.weight(1f).heightIn(min = 58.dp)
    val padding = PaddingValues(horizontal = 2.dp, vertical = 5.dp)
    if (primary) Button(onClick = onClick, enabled = enabled, modifier = tile,
        shape = UaiShapes.button, contentPadding = padding, content = content)
    else OutlinedButton(onClick = onClick, enabled = enabled, modifier = tile,
        shape = UaiShapes.button, contentPadding = padding, content = content)
}
