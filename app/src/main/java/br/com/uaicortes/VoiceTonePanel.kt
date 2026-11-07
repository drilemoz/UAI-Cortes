package br.com.uaicortes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import java.util.Locale

internal data class VoiceToneSource(
    val id: String, val label: String, val uri: String,
    val sourceStartMs: Long, val durationMs: Long, val volume: Float,
    val tone: VoiceTone
)

internal fun voiceToneSources(project: VideoProject): List<VoiceToneSource> = buildList {
    project.clips.filterNot { it.isImage }.forEachIndexed { index, clip ->
        add(VoiceToneSource("clip:${clip.id}", "Vídeo ${index + 1}: ${clip.title}", clip.uri,
            clip.startMs, clip.durationMs, clip.volume, clip.voiceTone))
    }
    ClipAudioBindings.resolved(project).forEach { track ->
        add(VoiceToneSource("track:${track.id}", "Áudio: ${track.title}", track.uri,
            track.sourceStartMs, track.durationMs, track.volume, track.voiceTone))
    }
}

private enum class VoiceSettingsPage { STYLE, TREATMENT, EQUALIZER, CLEANUP, PROFILES }

private fun voicePresetLabel(preset: String): String = when (preset) {
    VoiceTone.NATURAL -> "Natural"
    VoiceTone.WARM -> "Encorpada"
    VoiceTone.CLEAR -> "Clara"
    VoiceTone.STUDIO -> "Estúdio"
    else -> "Desligado"
}

/**
 * Edição não destrutiva por fonte. A tela principal mostra apenas status, atalhos e A/B.
 * Controles avançados ficam em folhas modais alinhadas ao design do editor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VoiceTonePanel(project: VideoProject, update: (VideoProject) -> Unit,
                            onPreview: (String, Boolean) -> Unit,
                            previewKey: String) {
    val sources = voiceToneSources(project)
    val context = LocalContext.current
    val favorites = remember(context) { VoiceToneFavorites(context) }
    var selectedId by remember(project.id) { mutableStateOf<String?>(null) }
    var expanded by remember(project.id) { mutableStateOf(true) }
    var activePage by remember(project.id) { mutableStateOf<VoiceSettingsPage?>(null) }
    var menu by remember(project.id) { mutableStateOf(false) }
    var showCopyDialog by remember(project.id) { mutableStateOf(false) }
    var selectedSlot by remember(project.id) { mutableIntStateOf(1) }
    var feedback by remember(project.id) { mutableStateOf("") }
    val selected = sources.firstOrNull { it.id == selectedId } ?: sources.firstOrNull()
    val tone = selected?.tone?.safe() ?: VoiceTone()

    fun change(value: VoiceTone) {
        val id = selected?.id ?: return
        val safe = value.safe()
        if (id.startsWith("clip:")) {
            val clipId = id.removePrefix("clip:")
            update(project.copy(clips = project.clips.map { clip ->
                if (clip.id == clipId) clip.copy(voiceTone = safe) else clip
            }))
        } else {
            val trackId = id.removePrefix("track:")
            update(project.copy(audioTracks = project.audioTracks.map { track ->
                if (track.id == trackId) track.copy(voiceTone = safe) else track
            }))
        }
    }

    AudioPanelCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("Melhorar voz", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(when {
                    sources.isEmpty() -> "Adicione um vídeo ou áudio para começar"
                    sources.any { it.tone.enabled } -> "Ativo em ${sources.count { it.tone.enabled }} faixa(s)"
                    else -> "Nenhuma faixa com tratamento ativo"
                }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { expanded = !expanded }, enabled = sources.isNotEmpty()) {
                Text(if (expanded) "Recolher" else "Abrir")
            }
        }
        if (expanded && selected != null) {
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth(),
                    shape = UaiShapes.button) {
                    Text(selected.label, modifier = Modifier.weight(1f), maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Escolher faixa")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    sources.forEach { source ->
                        DropdownMenuItem(text = { Text(source.label, maxLines = 2) }, onClick = {
                            selectedId = source.id
                            menu = false
                            feedback = ""
                        })
                    }
                }
            }
            Surface(shape = UaiShapes.item,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(voicePresetLabel(tone.preset), fontWeight = FontWeight.SemiBold)
                        Text(if (tone.enabled)
                            "Intensidade ${(tone.intensity * 100).roundToInt()}% · Compressão ${(tone.compression * 100).roundToInt()}%"
                            else "Toque em Estilo para ativar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2)
                    }
                    Icon(if (tone.enabled) Icons.Default.CheckCircle else Icons.Default.Tune,
                        contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceSettingsAction("Estilo", Icons.Default.Tune,
                    Modifier.weight(1f)) { activePage = VoiceSettingsPage.STYLE }
                VoiceSettingsAction("Ajustes", Icons.Default.Settings,
                    Modifier.weight(1f), enabled = tone.preset != VoiceTone.OFF) {
                    activePage = VoiceSettingsPage.TREATMENT
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceSettingsAction("Equalizador", Icons.Default.GraphicEq,
                    Modifier.weight(1f), enabled = tone.preset != VoiceTone.OFF) {
                    activePage = VoiceSettingsPage.EQUALIZER
                }
                VoiceSettingsAction("Limpeza", Icons.Default.AutoFixHigh,
                    Modifier.weight(1f), enabled = tone.preset != VoiceTone.OFF) {
                    activePage = VoiceSettingsPage.CLEANUP
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { activePage = VoiceSettingsPage.PROFILES }) {
                    Icon(Icons.Default.BookmarkBorder, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Meus ajustes")
                }
                Spacer(Modifier.weight(1f))
                if (project.clips.count { !it.isImage } > 1 && tone.enabled) {
                    TextButton(onClick = { showCopyDialog = true }) {
                        Text("Copiar para vídeos")
                    }
                }
            }
            if (feedback.isNotBlank()) Text(feedback,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onPreview(selected.id, false) },
                    modifier = Modifier.weight(1f), shape = UaiShapes.button,
                    contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(if (previewKey == "${selected.id}:original") Icons.Default.Pause
                         else Icons.Default.PlayArrow, contentDescription = null,
                        modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Original", maxLines = 1, fontSize = 12.sp)
                }
                Button(onClick = { onPreview(selected.id, true) },
                    modifier = Modifier.weight(1f), shape = UaiShapes.button,
                    enabled = tone.enabled, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(if (previewKey == "${selected.id}:treated") Icons.Default.Pause
                         else Icons.Default.PlayArrow, contentDescription = null,
                        modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tratada", maxLines = 1, fontSize = 12.sp)
                }
            }
        }
    }

    if (activePage != null && selected != null) {
        val page = activePage!!
        val sheetHeight = (LocalConfiguration.current.screenHeightDp * 0.78f).dp
        ModalBottomSheet(onDismissRequest = { activePage = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = UaiShapes.sheet,
            containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().heightIn(max = sheetHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(when (page) {
                        VoiceSettingsPage.STYLE -> "Estilo de voz"
                        VoiceSettingsPage.TREATMENT -> "Ajustes de voz"
                        VoiceSettingsPage.EQUALIZER -> "Equalizador (EQ) avançado"
                        VoiceSettingsPage.CLEANUP -> "Limpeza e sibilância"
                        VoiceSettingsPage.PROFILES -> "Meus ajustes"
                    }, style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = { activePage = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar ajustes")
                    }
                }
                when (page) {
                    VoiceSettingsPage.STYLE -> {
                        listOf(
                            Triple(VoiceTone.OFF, "Desligado", "Sem tratamento de voz"),
                            Triple(VoiceTone.NATURAL, "Natural", "Equilíbrio discreto"),
                            Triple(VoiceTone.WARM, "Encorpada", "Mais corpo e presença"),
                            Triple(VoiceTone.CLEAR, "Clara", "Fala mais definida"),
                            Triple(VoiceTone.STUDIO, "Estúdio", "Corpo e clareza equilibrados")
                        ).forEach { (preset, label, description) ->
                            Surface(onClick = { change(tone.copy(preset = preset)); activePage = null },
                                shape = UaiShapes.item,
                                color = if (tone.preset == preset)
                                    MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp,
                                    if (tone.preset == preset) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant)) {
                                Row(Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = tone.preset == preset, onClick = null)
                                    Spacer(Modifier.width(9.dp))
                                    Column {
                                        Text(label, fontWeight = FontWeight.SemiBold)
                                        Text(description, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    VoiceSettingsPage.TREATMENT -> {
                        VoiceUnitSlider("Intensidade", tone.intensity) {
                            change(tone.copy(intensity = it))
                        }
                        VoiceUnitSlider("Compressão", tone.compression) {
                            change(tone.copy(compression = it))
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Compensar volume")
                                Text("Recupera parte do volume após comprimir",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = tone.autoGain,
                                onCheckedChange = { change(tone.copy(autoGain = it)) })
                        }
                    }
                    VoiceSettingsPage.EQUALIZER -> {
                        Text("Ajuste o timbre sem alterar a altura da voz.",
                            style = MaterialTheme.typography.bodySmall)
                        VoiceEqSlider("Graves", tone.bassDb) { change(tone.copy(bassDb = it)) }
                        VoiceEqSlider("Corpo / médios", tone.bodyDb) { change(tone.copy(bodyDb = it)) }
                        VoiceEqSlider("Clareza", tone.clarityDb) { change(tone.copy(clarityDb = it)) }
                        VoiceEqSlider("Brilho", tone.trebleDb) { change(tone.copy(trebleDb = it)) }
                        OutlinedButton(onClick = { change(tone.copy(bassDb = 0f, bodyDb = 0f,
                            clarityDb = 0f, trebleDb = 0f)) },
                            shape = UaiShapes.button) { Text("Zerar EQ manual") }
                    }
                    VoiceSettingsPage.CLEANUP -> {
                        VoiceUnitSlider("Redução de ruído leve", tone.noiseReduction) {
                            change(tone.copy(noiseReduction = it))
                        }
                        VoiceUnitSlider("Suavizar S e CH", tone.deEsser) {
                            change(tone.copy(deEsser = it))
                        }
                        Text("A limpeza usa um expander suave, não RNNoise. Valores altos podem cortar palavras baixas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    VoiceSettingsPage.PROFILES -> {
                        Text("Salve até três perfis neste aparelho.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..3).forEach { slot ->
                                FilterChip(selected = selectedSlot == slot,
                                    onClick = { selectedSlot = slot },
                                    label = { Text("Perfil $slot") }, modifier = Modifier.weight(1f))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                favorites.save(selectedSlot, tone)
                                feedback = "Perfil $selectedSlot salvo neste aparelho."
                            }, modifier = Modifier.weight(1f),
                                shape = UaiShapes.button, enabled = tone.enabled) {
                                Text("Salvar ajuste", maxLines = 2)
                            }
                            Button(onClick = {
                                val loaded = favorites.load(selectedSlot)
                                feedback = if (loaded == null) "Perfil $selectedSlot ainda está vazio."
                                else {
                                    change(loaded)
                                    "Perfil $selectedSlot aplicado."
                                }
                            }, modifier = Modifier.weight(1f), shape = UaiShapes.button) {
                                Text("Usar ajuste", maxLines = 2)
                            }
                        }
                        if (feedback.isNotBlank()) Text(feedback,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(onClick = { activePage = null }, modifier = Modifier.fillMaxWidth(),
                    shape = UaiShapes.button) { Text("Concluído") }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
    if (showCopyDialog) {
        AlertDialog(onDismissRequest = { showCopyDialog = false },
            shape = UaiShapes.dialog, title = { Text("Copiar tratamento?") },
            text = { Text("Copiar ajuste para outros vídeos deste projeto? " +
                "Faixas de áudio e música de fundo não serão alteradas.") },
            confirmButton = { TextButton(onClick = {
                update(project.copy(clips = project.clips.map {
                    if (it.isImage) it else it.copy(voiceTone = tone)
                }))
                showCopyDialog = false
                feedback = "Ajuste aplicado aos vídeos."
            }) { Text("Aplicar") } },
            dismissButton = { TextButton(onClick = { showCopyDialog = false }) {
                Text("Cancelar")
            } })
    }
}

@Composable
private fun VoiceSettingsAction(label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled,
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 8.dp),
        shape = UaiShapes.button) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, maxLines = 1, fontSize = 12.sp, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun VoiceUnitSlider(label: String, value: Float, change: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Text("${(value * 100).roundToInt()}%", fontSize = 13.sp)
    }
    Slider(value = value, onValueChange = change, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun VoiceEqSlider(label: String, db: Float, change: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Text("${if (db >= 0) "+" else ""}${String.format(Locale.ROOT, "%.1f", db)} dB",
            fontSize = 13.sp)
    }
    Slider(value = db, onValueChange = change, valueRange = -6f..6f,
        modifier = Modifier.fillMaxWidth())
}
