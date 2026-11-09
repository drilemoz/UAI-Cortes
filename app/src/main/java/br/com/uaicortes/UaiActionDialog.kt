package br.com.uaicortes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog

/** Janela Uai Cortes reutilizável: ações grandes, sem bloquear textos nas telas pequenas. */
@Composable
internal fun UaiActionDialog(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.CheckCircleOutline,
    primaryLabel: String,
    secondaryLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(Modifier.size(54.dp).background(MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(27.dp))
                }
                Text(title, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                content()
                Spacer(Modifier.height(2.dp))
                Button(onClick = onConfirm, enabled = confirmEnabled,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                    Text(primaryLabel, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                    Text(secondaryLabel)
                }
            }
        }
    }
}
