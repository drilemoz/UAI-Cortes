package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorTimelineHeader(positionMs: Long, durationMs: Long, zoom: Int,
    magnetic: Boolean, canUndo: Boolean, canRedo: Boolean,
    onZoom: (Int) -> Unit, onMagnet: () -> Unit, onUndo: () -> Unit, onRedo: () -> Unit,
    onSeek: (Long) -> Unit, onSeekOpened: () -> Unit) {
    var seekOpen by remember { mutableStateOf(false) }
    var zoomOpen by remember { mutableStateOf(false) }
    var optionsOpen by remember { mutableStateOf(false) }
    val positionLabel = NumericTime.format(NumericTime.fromMs(positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))))
    val durationLabel = NumericTime.format(NumericTime.fromMs(durationMs))
    val magneticDescription = "Encaixe magnético ${if (magnetic) "ligado" else "desligado"}"

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val layout = EditorTimelineHeaderPolicy.layout(maxWidth.value, LocalDensity.current.fontScale)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onSeekOpened(); seekOpen = true }, enabled = durationMs > 0L,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                    contentDescription = "Ir para um horário. Posição $positionLabel de $durationLabel"
                }, contentPadding = PaddingValues(horizontal = 2.dp)) {
                Text(if (layout == EditorTimelineHeaderPolicy.Layout.DIRECT) "$positionLabel / $durationLabel"
                    else "$positionLabel/$durationLabel",
                    style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (layout != EditorTimelineHeaderPolicy.Layout.MENU) {
                Box {
                    TextButton(onClick = { zoomOpen = true }, modifier = Modifier.heightIn(min = 48.dp).semantics {
                        contentDescription = "Zoom da linha do tempo, $zoom vezes"
                    }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text(if (layout == EditorTimelineHeaderPolicy.Layout.DIRECT) "Zoom ${zoom}×" else "${zoom}×",
                            style = MaterialTheme.typography.labelMedium)
                    }
                    DropdownMenu(expanded = zoomOpen, onDismissRequest = { zoomOpen = false }) {
                        TimelineZoomItems(zoom) { level -> onZoom(level); zoomOpen = false }
                    }
                }
            }
            if (layout == EditorTimelineHeaderPolicy.Layout.DIRECT) {
                IconButton(onClick = onMagnet, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Link, magneticDescription, Modifier.size(18.dp),
                        tint = if (magnetic) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else Box {
                IconButton(onClick = { optionsOpen = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.MoreHoriz, "Mais opções da linha do tempo", Modifier.size(20.dp))
                }
                DropdownMenu(expanded = optionsOpen, onDismissRequest = { optionsOpen = false }) {
                    if (layout == EditorTimelineHeaderPolicy.Layout.MENU) {
                        TimelineZoomItems(zoom) { level -> onZoom(level); optionsOpen = false }
                        HorizontalDivider()
                    }
                    DropdownMenuItem(text = { Text(magneticDescription) },
                        leadingIcon = { Icon(if (magnetic) Icons.Default.Check else Icons.Default.Link, null) },
                        onClick = { onMagnet(); optionsOpen = false })
                }
            }
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Undo, "Desfazer", Modifier.size(18.dp))
            }
            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Redo, "Refazer", Modifier.size(18.dp))
            }
        }
    }
    if (seekOpen && durationMs > 0L) EditorTimelineSeekSheet(positionMs, durationMs, durationLabel,
        onDismiss = { seekOpen = false }, onSeek = { ms -> onSeek(ms); seekOpen = false })
}

@Composable
private fun TimelineZoomItems(zoom: Int, onChoose: (Int) -> Unit) {
    EditorTimelineHeaderPolicy.zoomLevels.forEach { level ->
        DropdownMenuItem(text = { Text("Zoom ${level}×") },
            leadingIcon = { if (zoom == level) Icon(Icons.Default.Check, "Selecionado") },
            onClick = { onChoose(level) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTimelineSeekSheet(positionMs: Long, durationMs: Long, durationLabel: String,
    onDismiss: () -> Unit, onSeek: (Long) -> Unit) {
    var time by remember {
        mutableStateOf(TextFieldValue(NumericTime.fromMs(positionMs.coerceIn(0L, durationMs))))
    }
    var wasFocused by remember { mutableStateOf(false) }
    val target = EditorTimelineHeaderPolicy.position(time.text, durationMs)

    ModalBottomSheet(onDismissRequest = onDismiss, shape = UaiShapes.sheet,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ir para um horário", style = MaterialTheme.typography.titleLarge)
            Text("Duração do vídeo · $durationLabel", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(value = time, onValueChange = { next ->
                val digits = NumericTime.sanitize(next.text)
                time = next.copy(text = digits, selection = TextRange(
                    next.selection.start.coerceIn(0, digits.length), next.selection.end.coerceIn(0, digits.length)))
            }, modifier = Modifier.fillMaxWidth().onFocusChanged { focus ->
                if (focus.isFocused && !wasFocused) {
                    wasFocused = true
                    time = time.copy(selection = TextRange(0, time.text.length))
                }
            }, label = { Text("Horário") }, singleLine = true,
                visualTransformation = NumericTimeMask, isError = time.text.isNotBlank() && target == null,
                supportingText = { if (time.text.isNotBlank() && target == null) Text("Escolha um horário dentro do vídeo.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { target?.let(onSeek) }))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSeek(0L) }, modifier = Modifier.weight(1f)) { Text("Início") }
                OutlinedButton(onClick = { onSeek(durationMs) }, modifier = Modifier.weight(1f)) { Text("Fim") }
            }
            Button(onClick = { target?.let(onSeek) }, enabled = target != null,
                modifier = Modifier.fillMaxWidth()) { Text("Ir para") }
        }
    }
}
