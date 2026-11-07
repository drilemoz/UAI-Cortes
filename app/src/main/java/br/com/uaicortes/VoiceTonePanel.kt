package br.com.uaicortes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

internal data class VoiceToneSource(
    val id: String, val label: String, val uri: String,
    val sourceStartMs: Long, val durationMs: Long, val volume: Float,
    val tone: VoiceTone
)

internal fun voiceToneSources(project: VideoProject): List<VoiceToneSource> = buildList {
    project.clips.filterNot { it.isImage }.forEachIndexed { index, clip ->
        add(VoiceToneSource("clip:${clip.id}", "Vídeo ${index+1}: ${clip.title}", clip.uri,
            clip.startMs, clip.durationMs, clip.volume, clip.voiceTone))
    }
    ClipAudioBindings.resolved(project).forEach { track ->
        add(VoiceToneSource("track:${track.id}", "Áudio: ${track.title}", track.uri,
            track.sourceStartMs, track.durationMs, track.volume, track.voiceTone))
    }
}

/** Mantém a opção próxima da mixagem, sem obrigar o usuário a estudar um EQ antes de usar. */
@Composable
internal fun VoiceTonePanel(project: VideoProject, update: (VideoProject) -> Unit,
                            onPreview: (String, Boolean) -> Unit,
                            previewKey: String) {
    val sources = voiceToneSources(project)
    var selectedId by remember(project.id) { mutableStateOf<String?>(null) }
    var expanded by remember(project.id) { mutableStateOf(false) }
    var showEq by remember(project.id) { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val selected = sources.firstOrNull { it.id == selectedId } ?: sources.firstOrNull()
    val tone = selected?.tone?.safe() ?: VoiceTone()
    fun change(value: VoiceTone) {
        val id = selected?.id ?: return
        val safe = value.safe()
        if (id.startsWith("clip:")) {
            val clipId = id.removePrefix("clip:")
            update(project.copy(clips=project.clips.map { if (it.id==clipId) it.copy(voiceTone=safe) else it }))
        } else {
            val trackId = id.removePrefix("track:")
            update(project.copy(audioTracks=project.audioTracks.map { if (it.id==trackId) it.copy(voiceTone=safe) else it }))
        }
    }
    AudioPanelCard {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.GraphicEq,null,tint=MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("Melhorar voz",fontWeight=FontWeight.Bold,fontSize=16.sp)
                Text(if (sources.isEmpty()) "Adicione um vídeo ou áudio para começar"
                    else if (sources.any { it.tone.enabled }) "Tratamento ativo em ${sources.count { it.tone.enabled }} faixa(s)"
                    else "Sua voz, mais natural e encorpada",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick={expanded=!expanded},enabled=sources.isNotEmpty()) {
                Text(if(expanded) "Recolher" else "Ajustar")
            }
        }
        if (expanded && selected != null) {
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick={menu=true},modifier=Modifier.fillMaxWidth()) {
                    Text(selected.label,modifier=Modifier.weight(1f),maxLines=1)
                    Icon(Icons.Default.KeyboardArrowDown,null)
                }
                DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                    sources.forEach { source ->
                        DropdownMenuItem(text={Text(source.label,maxLines=2)},onClick={
                            selectedId=source.id;menu=false
                        })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf(VoiceTone.OFF to "Desligado", VoiceTone.NATURAL to "Natural",
                    VoiceTone.WARM to "Encorpada", VoiceTone.CLEAR to "Clara").forEach { (id, label) ->
                    FilterChip(selected=tone.preset==id,onClick={change(tone.copy(preset=id))},
                        label={Text(label)})
                }
            }
            if (tone.preset != VoiceTone.OFF) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Intensidade",modifier=Modifier.weight(1f),fontSize=13.sp)
                    Text("${(tone.intensity*100).roundToInt()}%",fontSize=12.sp)
                }
                Slider(value=tone.intensity,onValueChange={change(tone.copy(intensity=it))},
                    modifier=Modifier.height(30.dp))
                TextButton(onClick={showEq=!showEq},modifier=Modifier.align(Alignment.End)) {
                    Text(if (showEq) "Ocultar equalizador" else "Equalizador (EQ) avançado")
                }
                if (showEq) {
                    VoiceEqSlider("Graves", tone.bassDb) { change(tone.copy(bassDb=it)) }
                    VoiceEqSlider("Corpo / médios", tone.bodyDb) { change(tone.copy(bodyDb=it)) }
                    VoiceEqSlider("Clareza", tone.clarityDb) { change(tone.copy(clarityDb=it)) }
                    TextButton(onClick={change(tone.copy(bassDb=0f,bodyDb=0f,clarityDb=0f))}) {
                        Text("Zerar ajustes manuais")
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={onPreview(selected.id,false)},modifier=Modifier.weight(1f),
                    contentPadding=PaddingValues(horizontal=8.dp)) {
                    Icon(if(previewKey=="${selected.id}:original") Icons.Default.Pause
                         else Icons.Default.PlayArrow,null,modifier=Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp)); Text("Original",fontSize=12.sp)
                }
                Button(onClick={onPreview(selected.id,true)},modifier=Modifier.weight(1f),
                    enabled=tone.enabled,contentPadding=PaddingValues(horizontal=8.dp)) {
                    Icon(if(previewKey=="${selected.id}:treated") Icons.Default.Pause
                         else Icons.Default.PlayArrow,null,modifier=Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp)); Text("Tratada",fontSize=12.sp)
                }
            }
            Text("Só a faixa escolhida recebe o efeito. O original é preservado; " +
                "a exportação utiliza os mesmos filtros da prévia.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VoiceEqSlider(label:String, db:Float, change:(Float)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(label,modifier=Modifier.weight(1f),fontSize=13.sp)
        Text("${if(db>=0) "+" else ""}${String.format(java.util.Locale.ROOT,"%.1f",db)} dB",
            fontSize=12.sp)
    }
    Slider(value=db,onValueChange=change,valueRange=-4f..4f,
        modifier=Modifier.height(28.dp))
}
