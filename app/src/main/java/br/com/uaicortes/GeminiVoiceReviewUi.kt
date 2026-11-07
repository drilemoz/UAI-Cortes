package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Revisão local: a API apenas distingue vozes, não descobre nomes/personagens reais. */
@Composable
internal fun GeminiVoiceReviewUi(cues: List<SubtitleCue>, existing: List<SubtitleSpeaker>,
    decisions: Map<String,String>, names: Map<String,String>,
    onDecision: (String,String)->Unit, onName: (String,String)->Unit) {
    val ids=GeminiDiarizationPolicy.suggestions(cues)
    if (ids.isEmpty()) return
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Vozes sugeridas (${ids.size})", fontWeight=FontWeight.SemiBold,
                modifier=Modifier.weight(1f))
            SubtitleHelpButton("Revisar vozes sugeridas",
                "Voz 1, Voz 2 etc. são rótulos anônimos, não nomes de pessoas. " +
                "Confira as falas no vídeo; três ou mais locutores podem ser confundidos. " +
                "Você pode criar um personagem, associar a um existente ou não atribuir. " +
                "Nada é aplicado antes da confirmação final. " +
                "As cores exportadas só mudam se você usar Aplicar cores em Personagens.")
        }
        ids.forEach { id ->
            val selected=decisions[id] ?: GeminiDiarizationPolicy.CREATE
            var expanded by remember(id) { mutableStateOf(false) }
            val defaultName=GeminiDiarizationPolicy.label(id).orEmpty()
            val quantity=cues.count { it.speakerId == id }
            Surface(shape=RoundedCornerShape(10.dp),
                color=MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver,null)
                        Spacer(Modifier.width(6.dp))
                        Text("$defaultName · $quantity legenda(s)",fontWeight=FontWeight.Medium)
                    }
                    Box {
                        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth(), shape = UaiShapes.button) {
                            val label=when (selected) {
                                GeminiDiarizationPolicy.CREATE -> "Criar personagem para $defaultName"
                                GeminiDiarizationPolicy.IGNORE -> "Deixar sem personagem"
                                else -> "Associar: " + existing.firstOrNull {it.id == selected}?.name.orEmpty()
                            }
                            Text(label)
                        }
                        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                            DropdownMenuItem(text={Text("Criar novo personagem")},onClick={
                                onDecision(id,GeminiDiarizationPolicy.CREATE);expanded=false
                            })
                            DropdownMenuItem(text={Text("Não atribuir personagem")},onClick={
                                onDecision(id,GeminiDiarizationPolicy.IGNORE);expanded=false
                            })
                            existing.forEach { speaker ->
                                DropdownMenuItem(text={Text("Vincular a ${speaker.name}")},onClick={
                                    onDecision(id,speaker.id);expanded=false
                                })
                            }
                        }
                    }
                    if(selected==GeminiDiarizationPolicy.CREATE) {
                        OutlinedTextField(value=names[id] ?: defaultName,
                            onValueChange={onName(id,it.take(40))},
                            label={Text("Nome sugerido (confirme ou renomeie)")},
                            singleLine=true,modifier=Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
