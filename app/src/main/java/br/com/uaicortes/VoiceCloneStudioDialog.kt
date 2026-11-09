package br.com.uaicortes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Catálogo global e síntese manual no servidor do usuário; não executa PyTorch no Android. */
@Composable
internal fun VoiceCloneStudioDialog(project: VideoProject, cue: SubtitleCue?,
    onSave: (VideoProject) -> Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    val latestProject by rememberUpdatedState(project)
    val scope = rememberCoroutineScope()
    val library = remember(context) { VoiceCloneLibrary(context) }
    val prefs = remember(context) { context.getSharedPreferences("voice_clone_bridge_v1", 0) }
    var profiles by remember { mutableStateOf(library.load()) }
    var voiceName by remember { mutableStateOf("") }
    var engine by remember { mutableStateOf(VoiceClonePolicy.CHATTERBOX) }
    val speaker = project.subtitleSpeakers.firstOrNull { it.id == cue?.speakerId }
    val assignedId = if (speaker != null) project.voiceIaVoices.speakerCloneProfiles[speaker.id]
        else cue?.id?.let { project.voiceIaVoices.cueCloneProfiles[it] }
    var selectedId by remember(project.id, cue?.id) { mutableStateOf(assignedId) }
    val active = profiles.firstOrNull { it.id == selectedId }
    var origin by remember { mutableStateOf(prefs.getString("url", "") ?: "") }
    var token by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<VoiceCloneProfile?>(null) }
    val referencePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && consent && !working) scope.launch {
            working = true
            try {
                val profile = library.add(context, uri, voiceName, engine)
                profiles = library.load()
                selectedId = profile.id
                message = "Referência privada salva: ${profile.name}. Selecione Vincular para fixá-la ao personagem."
                voiceName = ""
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { message = e.message ?: "Falha ao importar referência." }
            finally { working = false }
        }
    }
    Dialog(onDismissRequest = { if (!working) onClose() }) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().heightIn(max = 660.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Biblioteca de vozes clonadas", style = MaterialTheme.typography.titleMedium)
                Text("Coqui XTTS-v2 ou Chatterbox Multilingual V3 · Referências locais reutilizáveis em todos os projetos.",
                    style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("1. Criar referência", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = voiceName, onValueChange = { voiceName = it.take(60) },
                    label = { Text("Nome do personagem / voz") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), enabled = !working)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = engine == VoiceClonePolicy.CHATTERBOX,
                        onClick = { engine = VoiceClonePolicy.CHATTERBOX },
                        label = { Text("Chatterbox V3") }, enabled = !working)
                    FilterChip(selected = engine == VoiceClonePolicy.COQUI,
                        onClick = { engine = VoiceClonePolicy.COQUI },
                        label = { Text("Coqui XTTS-v2") }, enabled = !working)
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = consent, onCheckedChange = { consent = it }, enabled = !working)
                    Text("Tenho autorização para clonar e usar esta voz.",
                        style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = { referencePicker.launch(arrayOf("audio/wav", "audio/x-wav", "audio/*")) },
                    enabled = !working && consent && voiceName.isNotBlank()) {
                    Text("Importar WAV de 3 a 30 segundos")
                }
                HorizontalDivider()
                Text("2. Escolher voz salva", fontWeight = FontWeight.SemiBold)
                if (profiles.isEmpty()) Text("Nenhuma voz cadastrada.")
                profiles.forEach { profile ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = selectedId == profile.id,
                            onClick = { selectedId = profile.id }, enabled = !working)
                        Column(Modifier.weight(1f)) {
                            Text(profile.name, style = MaterialTheme.typography.bodyMedium)
                            Text(if (profile.engine == VoiceClonePolicy.CHATTERBOX) "Chatterbox V3" else "Coqui XTTS-v2",
                                style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { deleteTarget = profile }, enabled = !working) { Text("Excluir") }
                    }
                }
                if (cue != null) {
                    Text("Fala: ${cue.text.take(110)}", style = MaterialTheme.typography.bodySmall)
                    Text("Destino: ${speaker?.name ?: "Fala sem personagem"}",
                        style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            val id = selectedId ?: return@OutlinedButton
                            val saved = project.voiceIaVoices
                            val revised = if (speaker != null) saved.copy(
                                speakerCloneProfiles = saved.speakerCloneProfiles + (speaker.id to id))
                            else saved.copy(cueCloneProfiles = saved.cueCloneProfiles + (cue.id to id))
                            message = if (onSave(project.copy(voiceIaVoices = revised)))
                                "Voz vinculada ao personagem/fala neste projeto." else "Falha ao salvar vínculo."
                        }, enabled = !working && active != null) { Text("Fixar voz") }
                        if (assignedId != null) TextButton(onClick = {
                            val saved = project.voiceIaVoices
                            val revised = if (speaker != null) saved.copy(
                                speakerCloneProfiles = saved.speakerCloneProfiles - speaker.id)
                            else saved.copy(cueCloneProfiles = saved.cueCloneProfiles - cue.id)
                            message = if (onSave(project.copy(voiceIaVoices = revised)))
                                "Vínculo removido, sem apagar áudio existente." else "Falha ao salvar."
                        }, enabled = !working) { Text("Desvincular") }
                    }
                    if (assignedId != null) Text("Voz fixa: ${profiles.firstOrNull { it.id == assignedId }?.name ?: "referência indisponível"}",
                        style = MaterialTheme.typography.labelSmall)
                }
                HorizontalDivider()
                Text("3. Gerar fala no servidor", fontWeight = FontWeight.SemiBold)
                Text("Serviço Python externo, HTTPS com certificado válido. O áudio de referência e o texto serão enviados somente ao servidor configurado após confirmar.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = origin, onValueChange = { origin = it.take(200) },
                    label = { Text("Servidor HTTPS (ex.: https://voz.exemplo.com)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !working)
                OutlinedTextField(value = token, onValueChange = { token = it.take(256) },
                    label = { Text("Token do servidor (somente nesta sessão)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !working,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                Button(onClick = {
                    val profile = active ?: return@Button
                    val currentCue = cue ?: return@Button
                    working = true
                    message = "Gerando. A chamada não será repetida automaticamente em caso de erro."
                    scope.launch {
                        var output: java.io.File? = null
                        try {
                            val normalized = VoiceClonePolicy.checkEndpoint(origin)
                            prefs.edit().putString("url", normalized).apply()
                            val generated = VoiceCloneClient.synthesize(context, normalized, token, profile,
                                currentCue.text)
                            output = generated
                            val current = latestProject
                            require(current.id == project.id) { "Projeto alterado durante a geração." }
                            val audio = withContext(Dispatchers.IO) {
                                VoiceCloneAudio.prepare(generated, current, currentCue, profile)
                            }
                            val revised = current.copy(audioTracks = current.audioTracks.filterNot {
                                it.kind == VoiceIaPolicy.KIND && it.subtitleId == currentCue.id
                            } + audio)
                            if (!onSave(revised)) throw IllegalStateException("Falha ao salvar a faixa no projeto.")
                            output = null // Arquivo agora pertence ao projeto; não apagar.
                            message = "WAV gerado e inserido na timeline. Reprodução disponível na lista de falas."
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) {
                            message = (e.message ?: "Falha ao gerar a voz.") +
                                " Nenhuma repetição automática foi feita."
                        } finally { withContext(Dispatchers.IO) { output?.delete() }; working = false }
                    }
                }, enabled = !working && active != null && cue != null && origin.isNotBlank() && token.isNotBlank()) {
                    Text(if (working) "Gerando…" else "Gerar e inserir esta fala")
                }
                if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
                Text("A voz clonada é uma aproximação. Coqui XTTS-v2 tem licença de modelo com restrições comerciais; verifique o uso antes de monetizar.",
                    style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = onClose, enabled = !working) { Text("Fechar") }
            }
        }
    }
    deleteTarget?.let { profile ->
        AlertDialog(onDismissRequest = { deleteTarget = null },
            title = { Text("Excluir referência ${profile.name}?") },
            text = { Text("Projetos que usam esta voz perderão a referência para novas falas. Os WAVs já gerados permanecem.") },
            confirmButton = { TextButton(onClick = {
                runCatching { library.remove(profile) }.onSuccess {
                    profiles = library.load()
                    if (selectedId == profile.id) selectedId = null
                    message = "Referência excluída."
                }.onFailure { message = it.message ?: "Não foi possível excluir." }
                deleteTarget = null
            }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } })
    }
}
