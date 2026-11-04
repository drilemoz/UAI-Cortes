package br.com.uaicortes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Publicação é metadado do projeto: jamais altera MP4, clipes ou legendas. */
@Composable
internal fun ProjectPublicationScreen(
    project: VideoProject,
    onSave: (VideoPublicationDraft) -> Boolean,
    back: () -> Unit
) {
    val context = LocalContext.current
    var description by remember(project.id) {
        mutableStateOf(project.publicationDraft?.description.orEmpty())
    }
    var hashtags by remember(project.id) {
        mutableStateOf(project.publicationDraft?.hashtags.orEmpty())
    }
    var saved by remember(project.id) { mutableStateOf(project.publicationDraft ?: VideoPublicationDraft()) }
    var error by remember(project.id) { mutableStateOf(false) }
    var copied by remember(project.id) { mutableStateOf(false) }
    val latestSave by rememberUpdatedState(onSave)
    fun current() = VideoPublicationDraft(description, hashtags)
    fun persist(): Boolean {
        val draft = current()
        if (draft == saved) return true
        val success = runCatching { latestSave(draft) }.getOrDefault(false)
        if (success) saved = draft
        error = !success
        return success
    }
    LaunchedEffect(project.id, description, hashtags) {
        if (current() != saved) {
            delay(650)
            persist()
        }
    }
    BackHandler { if (persist()) back() }
    val valid = VideoCaptionPolicy.isValid(description, hashtags)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        .verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (persist()) back() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar ao editor")
            }
            Column(Modifier.weight(1f)) {
                Text("Descrição para publicar", style = MaterialTheme.typography.titleLarge)
                Text(project.title, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Você pode editar e copiar o texto sem exportar o vídeo de novo nem usar a API Gemini.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = description, onValueChange = {
            description = it.take(VideoPublicationDraft.MAX_FIELD_LENGTH); copied = false
        }, modifier = Modifier.fillMaxWidth(), label = { Text("Descrição") },
            minLines = 2, maxLines = 4, isError = description.isNotBlank() &&
                (description.contains('\n') || description.contains('#') ||
                    VideoCaptionPolicy.containsEmoticonOrEmoji(description)))
        OutlinedTextField(value = hashtags, onValueChange = {
            hashtags = it.take(VideoPublicationDraft.MAX_FIELD_LENGTH); copied = false
        }, modifier = Modifier.fillMaxWidth(), label = { Text("5 hashtags") },
            minLines = 2, maxLines = 3, isError = hashtags.isNotBlank() &&
                !VideoCaptionPolicy.isValid(description, hashtags))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${VideoCaptionPolicy.hashtagCount(hashtags)}/5 hashtags",
                style = MaterialTheme.typography.labelSmall)
            Text("${VideoCaptionPolicy.totalCharacters(description, hashtags)}/100 caracteres",
                style = MaterialTheme.typography.labelSmall,
                color = if (VideoCaptionPolicy.totalCharacters(description, hashtags) > 100)
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!valid) Text("Para copiar, use uma descrição, cinco hashtags distintas, no máximo 100 caracteres no total e nenhum emoji ou emoticon.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        if (error) Text("Não foi possível salvar o texto no projeto. Verifique o armazenamento antes de sair.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall)
        else Text(if (current() == saved) "Texto salvo no projeto."
            else "Alterações pendentes: salvamento automático em instantes.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { persist() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Salvar")
            }
            Button(onClick = {
                persist()
                val combined = VideoCaption(description.trim(), hashtags.trim()).combined
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                    ?.setPrimaryClip(ClipData.newPlainText("Descrição e hashtags", combined))
                copied = true
            }, enabled = valid, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copiar")
            }
        }
        if (copied) Text("Descrição e hashtags copiadas.", style = MaterialTheme.typography.labelSmall)
    }
}
