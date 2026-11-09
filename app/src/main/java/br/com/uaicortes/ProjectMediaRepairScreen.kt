package br.com.uaicortes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Assistente não destrutivo de recuperação: um arquivo por vez, com validação prévia. */
@Composable
internal fun ProjectMediaRepairScreen(
    project: VideoProject,
    back: () -> Unit,
    save: (VideoProject) -> Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries = remember(project) { ProjectMediaRelink.entries(project) }
    var missing by remember(project) { mutableStateOf<List<MediaRelinkEntry>?>(null) }
    var requestedKey by rememberSaveable(project.id) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    LaunchedEffect(project) {
        missing = withContext(Dispatchers.IO) {
            entries.filter { entry ->
                !ProjectMediaAvailability.canOpen(context.applicationContext, entry.originalUri)
            }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked: Uri? ->
        val target = entries.firstOrNull { it.key == requestedKey }
        requestedKey = ""
        if (picked != null && target != null && !busy) {
            scope.launch {
                busy = true
                var provisionalCopy: String? = null
                try {
                    val replacement = MediaRelinkVerification.prepare(context.applicationContext, picked, target)
                    if (target.mimePrefix == "image/") provisionalCopy = replacement
                    val corrected = ProjectMediaRelink.replace(project, target, replacement)
                    if (save(corrected)) {
                        provisionalCopy = null // Agora pertence ao projeto salvo.
                        feedback = "Arquivo reconectado. Cortes e horários preservados."
                        DiagnosticRecorder.action("media_repair", "source_relinked")
                    } else {
                        feedback = "Não foi possível salvar. O projeto original foi preservado."
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    DiagnosticRecorder.exception("media_repair_failure", failure)
                    feedback = "Não foi possível recuperar esse arquivo. Tente escolhê-lo novamente."
                } finally {
                    provisionalCopy?.let { raw ->
                        ImageClipImport.discardUnused(context.applicationContext,
                            ImageClipSource(raw, "", "image/png"))
                    }
                    busy = false
                }
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Heading("Reparar mídias", back)
        Text("Se um vídeo, foto ou áudio deixou de abrir, localize novamente o arquivo original. " +
            "Os cortes, posições, volume e legendas não serão alterados.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium)
        Text("Escolha o mesmo arquivo, não um vídeo diferente. O app confere formato e duração, " +
            "mas não consegue comprovar que o conteúdo é idêntico.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        when {
            missing == null -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                CircularProgressIndicator()
            }
            missing!!.isEmpty() -> Text("Todas as mídias cadastradas estão acessíveis.",
                Modifier.padding(20.dp))
            else -> {
                Text("${missing!!.size} item(ns) sem acesso", Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium)
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(missing!!, key = { it.key }) { target ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.BrokenImage, null)
                                Column(Modifier.weight(1f)) {
                                    Text(target.title, maxLines = 2)
                                    Text(when (target.kind) {
                                        "clip" -> "Clipe principal"
                                        "pip" -> "Sobreposição PiP"
                                        "music" -> "Música de fundo"
                                        else -> "Faixa de áudio/voz"
                                    }, style = MaterialTheme.typography.bodySmall)
                                }
                                Button(enabled = !busy, onClick = {
                                    requestedKey = target.key
                                    picker.launch(arrayOf("${target.mimePrefix}*"))
                                }) { Text("Localizar") }
                            }
                        }
                    }
                }
            }
        }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Conferindo mídia e salvando referência…", Modifier.padding(12.dp))
        }
        if (feedback.isNotBlank()) {
            Text(feedback, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.primary)
        }
        TextButton(onClick = back, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
            Text("Voltar ao editor")
        }
    }
}
