package br.com.uaicortes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Confirmação unificada após o MP4 estar efetivamente salvo no MediaStore. */
@Composable
internal fun VideoSavedDialog(
    uri: Uri, name: String, editLabel: String,
    onEdit: () -> Unit, onClose: () -> Unit
) {
    val context = LocalContext.current
    var failure by remember(uri) { mutableStateOf<String?>(null) }
    val displayedName = remember(uri,name) {
        if (name.isNotBlank()) name else runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME),
                null,null,null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: "UaiCortes_Animado.mp4"
    }
    Dialog(onDismissRequest=onClose, properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.Center) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(90.dp),
                    tint=MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(20.dp))
                Text("Vídeo salvo!", style=MaterialTheme.typography.headlineMedium,
                    fontWeight=FontWeight.Bold, textAlign=TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(displayedName,style=MaterialTheme.typography.titleMedium,textAlign=TextAlign.Center,
                    maxLines=3,overflow=TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text("Salvo na galeria em Filmes/UaiCortes",textAlign=TextAlign.Center,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Button(onClick={
                    runCatching {context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri,"video/mp4")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })}.onFailure {failure="Não há aplicativo disponível para abrir este vídeo."}
                },modifier=Modifier.fillMaxWidth()) {Text("▶ Abrir vídeo")}
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick={
                    runCatching {context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type="video/mp4"
                        putExtra(Intent.EXTRA_STREAM,uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },"Compartilhar vídeo"))}.onFailure {failure="Não foi possível compartilhar este vídeo."}
                },modifier=Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Share,null);Spacer(Modifier.width(8.dp));Text("Compartilhar")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick=onEdit,modifier=Modifier.fillMaxWidth()) {Text(editLabel)}
                TextButton(onClick=onClose) { Text("Fechar") }
                failure?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            }
        }
    }
}
