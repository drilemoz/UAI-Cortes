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
        Text("Passo 3A · Revisar quem está falando", fontWeight=FontWeight.SemiBold)
        Text("O Gemini sugeriu ${ids.size} voz(es). Voz 1, Voz 2 etc. são rótulos " +
            "anônimos, não nomes de pessoas. Confira ouvindo o vídeo; 3 ou mais " +
            "vozes podem ser confundidas. Nenhuma associação será aplicada antes " +
            "de tocar em Aplicar.", style=MaterialTheme.typography.bodySmall)
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
                        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {
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
        Text("As cores e os nomes ficam no projeto, mas não mudam a cor das " +
            "legendas exportadas automaticamente. Use Personagens para aplicar cores " +
            "depois, se quiser.",style=MaterialTheme.typography.bodySmall)
    }
}
