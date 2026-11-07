package br.com.uaicortes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.ComponentActivity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
internal fun SocialAddDestinationDialog(metaAvailable: Boolean, onDismiss: () -> Unit,
                                       onYouTube: () -> Unit, onFacebook: () -> Unit,
                                       onManual: (SocialDestination) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Adicionar conta ou destino") },
        text = { Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CONTAS COM CONEXÃO", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onYouTube, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.SmartDisplay, null); Spacer(Modifier.width(8.dp))
                Text("Conectar canal YouTube")
            }
            OutlinedButton(onClick = onFacebook, modifier = Modifier.fillMaxWidth(),
                enabled = metaAvailable) {
                Icon(Icons.Default.Pages, null); Spacer(Modifier.width(8.dp))
                Text("Conectar Página Facebook")
            }
            if (!metaAvailable) Text("Facebook Página: configure Meta no build para conectar automaticamente.",
                style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("COMPARTILHAMENTO MANUAL", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(SocialDestination.FACEBOOK_PROFILE, SocialDestination.TIKTOK,
                SocialDestination.KWAI).forEach { dest ->
                TextButton(onClick = { onManual(dest) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Adicionar ${dest.label}")
                }
            }
            TextButton(onClick = { onManual(SocialDestination.FACEBOOK_PAGE) }) {
                Text("Cadastrar Página manualmente (avançado)")
            }
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } })
}

@Composable
internal fun SocialAccountDialog(initialDestination: SocialDestination,
                                onDismiss: () -> Unit, onSave: (SocialAccount) -> Unit) {
    val selected = initialDestination
    var label by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val valid = label.trim().length in 1..70 && SocialPublicationPolicy.isSafeHttps(url)
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Adicionar destino") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cadastro manual: ${selected.label}. Para YouTube, use Conectar canal.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = label, onValueChange = { label = it.take(70) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Nome da conta ou Página") },
                    singleLine = true)
                OutlinedTextField(value = url, onValueChange = { url = it.take(300) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Link HTTPS (opcional)") },
                    singleLine = true, isError = !SocialPublicationPolicy.isSafeHttps(url))
            }
        }, confirmButton = { TextButton(onClick = {
            onSave(SocialAccount(destination = selected, label = label.trim(), profileUrl = url.trim()))
        }, enabled = valid) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
internal fun SocialPublicationDetailDialog(current: SocialPublication, onDismiss: () -> Unit,
                                          onSave: (SocialPublication) -> Unit) {
    var confirmed by remember(current.id) {
        mutableStateOf(current.status == SocialPublicationStatus.CONFIRMED_MANUALLY)
    }
    var url by remember(current.id) { mutableStateOf(current.postUrl) }
    var views by remember(current.id) { mutableStateOf(current.metrics.views?.toString().orEmpty()) }
    var likes by remember(current.id) { mutableStateOf(current.metrics.likes?.toString().orEmpty()) }
    var comments by remember(current.id) { mutableStateOf(current.metrics.comments?.toString().orEmpty()) }
    var shares by remember(current.id) { mutableStateOf(current.metrics.shares?.toString().orEmpty()) }
    fun parse(s: String) = if (s.isBlank()) null else s.toLongOrNull()?.takeIf { it >= 0 }
    val metricsValid = listOf(views, likes, comments, shares).all {
        it.isBlank() || (it.toLongOrNull()?.let { n -> n >= 0 } == true)
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Status e métricas") },
        text = {
            Column(Modifier.heightIn(max = 490.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("O Android não confirma se o compartilhamento foi publicado. " +
                    "Marque apenas após conferir na rede.", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = confirmed, enabled = current.status != SocialPublicationStatus.CONFIRMED_BY_API,
                        onCheckedChange = { confirmed = it })
                    Text("Confirmo que publiquei")
                }
                OutlinedTextField(value = url, onValueChange = { url = it.take(300) },
                    label = { Text("Link da publicação (opcional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    isError = !SocialPublicationPolicy.isSafeHttps(url))
                Text("Métricas informadas manualmente (opcionais)",
                    style = MaterialTheme.typography.labelMedium)
                listOf("Visualizações" to views, "Curtidas" to likes,
                    "Comentários" to comments, "Compartilhamentos" to shares).forEach { (title, value) ->
                    OutlinedTextField(value = value, onValueChange = { input ->
                        val clean = input.filter(Char::isDigit).take(16)
                        when (title) {
                            "Visualizações" -> views = clean
                            "Curtidas" -> likes = clean
                            "Comentários" -> comments = clean
                            else -> shares = clean
                        }
                    }, label = { Text(title) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }, confirmButton = { TextButton(onClick = {
            val now = System.currentTimeMillis()
            val nextMetrics = SocialMetrics(parse(views), parse(likes), parse(comments), parse(shares),
                if (listOf(views, likes, comments, shares).any { it.isNotBlank() }) now else 0L)
            onSave(current.copy(status = when {
                current.status == SocialPublicationStatus.CONFIRMED_BY_API -> SocialPublicationStatus.CONFIRMED_BY_API
                current.status == SocialPublicationStatus.UPLOAD_REVIEW -> SocialPublicationStatus.UPLOAD_REVIEW
                confirmed -> SocialPublicationStatus.CONFIRMED_MANUALLY
                current.status == SocialPublicationStatus.CONFIRMED_MANUALLY -> SocialPublicationStatus.SHARED_UNCONFIRMED
                else -> current.status
            }, postUrl = if (current.status == SocialPublicationStatus.CONFIRMED_BY_API) current.postUrl else url.trim(),
                metrics = nextMetrics, updatedAt = now))
        }, enabled = SocialPublicationPolicy.isSafeHttps(url) && metricsValid) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

/** Novo vídeo ou MP4 exportado; cria uma tarefa por conta, nunca envia sem ação por destino. */
