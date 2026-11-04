package br.com.uaicortes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Sincronia por palavra sob controle do usuário; não chama IA nem analisa áudio. */
@Composable
internal fun SubtitleWordTimingEditor(cue: SubtitleCue,
    onDismiss: () -> Unit, onSave: (List<SubtitleWordTiming>) -> Unit) {
    val policy = SubtitleWordTimingEditorPolicy
    val words = remember(cue.text) { policy.words(cue) }
    var intervals by remember(cue.id, cue.startMs, cue.endMs, cue.text) {
        mutableStateOf(cue.wordTimings.takeIf { policy.valid(cue, it) }.orEmpty())
    }
    var selected by remember(cue.id) { mutableIntStateOf(0) }
    var message by remember(cue.id) { mutableStateOf("") }
    var seekMs by remember(cue.id) { mutableLongStateOf(cue.startMs) }
    val ready = policy.valid(cue, intervals)
    val previewCue = cue.copy(
        animation = if (SubtitleAnimations.isKaraoke(cue.animation)) cue.animation
            else SubtitleAnimations.KARAOKE,
        wordTimings = if (ready) intervals else emptyList())

    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth(.96f)
            .heightIn(max=LocalConfiguration.current.screenHeightDp.dp * .88f),
            shape=RoundedCornerShape(18.dp),tonalElevation=5.dp) {
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Sincronizar palavras",style=MaterialTheme.typography.titleMedium,
                    fontWeight=FontWeight.Bold)
                Text("Ajuste quando cada palavra é destacada. Os horários são manuais, " +
                    "não são detectados pelo áudio.",fontSize=12.sp,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                if (!policy.editable(cue)) {
                    Text("Para editar, use uma legenda de 1 a 40 palavras, " +
                        "com pelo menos 60 ms por palavra. Divida textos longos.",
                        color=MaterialTheme.colorScheme.error,fontSize=12.sp)
                } else {
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        OutlinedButton(onClick={
                            intervals=policy.distribute(cue)
                            selected=0
                            seekMs=intervals.firstOrNull()?.startMs ?: cue.startMs
                            message="Distribuição aproximada. Ajuste os tempos ouvindo o vídeo."
                        },contentPadding=PaddingValues(horizontal=10.dp,vertical=7.dp)) {
                            Text(if(ready) "Redistribuir" else "Distribuir tempos",fontSize=12.sp)
                        }
                        Text(if(ready) "${words.size} palavras" else "Sem tempos editáveis",
                            color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=11.sp)
                    }
                    if(message.isNotBlank()) Text(message,fontSize=11.sp,
                        color=MaterialTheme.colorScheme.primary)
                    if(ready) {
                        val frame=SubtitleAnimations.frame(previewCue,
                            seekMs.coerceIn(cue.startMs,cue.endMs))
                        Box(Modifier.fillMaxWidth().height(64.dp)
                            .background(Color(0xFF15171B),RoundedCornerShape(10.dp)),
                            contentAlignment=Alignment.Center) {
                            SubtitleStyledSample(previewCue.copy(sizeSp=18),frame,compact=true)
                        }
                        Text("Prévia: ${SubtitleAnimations.label(previewCue.animation)}",
                            fontSize=11.sp,color=MaterialTheme.colorScheme.primary)
                        Text("Toque em uma palavra para conferir o destaque. " +
                            "Ajuste a divisão entre palavras com − / + ou digite o horário.",
                            fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyColumn(Modifier.weight(1f,fill=false),
                            verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            itemsIndexed(words) { index,word ->
                                val interval=intervals[index]
                                val isCurrent=selected==index
                                Surface(onClick={
                                    selected=index
                                    seekMs=interval.startMs+(interval.endMs-interval.startMs)/2
                                },shape=RoundedCornerShape(9.dp),
                                    color=if(isCurrent) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant) {
                                    Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=7.dp),
                                        verticalAlignment=Alignment.CenterVertically) {
                                        Text("${index+1}. $word",fontWeight=if(isCurrent) FontWeight.SemiBold
                                            else FontWeight.Normal,modifier=Modifier.weight(1f),
                                            fontSize=12.sp,maxLines=1)
                                        Text("${formatPipMs(interval.startMs)} – " +
                                            formatPipMs(interval.endMs),fontSize=11.sp)
                                    }
                                }
                            }
                        }
                        if(selected in 0 until intervals.lastIndex) {
                            val current = intervals[selected]
                            val next=intervals[selected+1]
                            val boundary=current.endMs
                            val min=current.startMs+1L
                            val max=next.endMs-1L
                            Text("Divisão: ${words[selected]} / ${words[selected+1]}",
                                style=MaterialTheme.typography.labelMedium)
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                                horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick={
                                    policy.moveBoundary(cue,intervals,selected,
                                        (boundary-100L).coerceAtLeast(min))?.let {
                                        intervals=it;seekMs=it[selected+1].startMs
                                    }
                                },enabled=boundary>min,modifier=Modifier.weight(1f),
                                    contentPadding=PaddingValues(horizontal=2.dp)) {
                                    Text("−0,1 s",fontSize=12.sp)
                                }
                                OutlinedButton(onClick={
                                    policy.moveBoundary(cue,intervals,selected,
                                        (boundary+100L).coerceAtMost(max))?.let {
                                        intervals=it;seekMs=it[selected+1].startMs
                                    }
                                },enabled=boundary<max,modifier=Modifier.weight(1f),
                                    contentPadding=PaddingValues(horizontal=2.dp)) {
                                    Text("+0,1 s",fontSize=12.sp)
                                }
                            }
                            TimeDigitsField("Horário da divisão",boundary,cue.endMs,
                                valid={it>min-1L && it<max+1L},
                                onCommit={ms ->
                                    policy.moveBoundary(cue,intervals,selected,ms)?.let {
                                        intervals=it;seekMs=it[selected+1].startMs
                                    }
                                },compact=true)
                        } else {
                            Text("O último término segue o fim da legenda.",fontSize=11.sp,
                                color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text("Depois, toque em Salvar legenda para gravar no projeto.",
                    fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick=onDismiss,modifier=Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(onClick={onSave(intervals)},enabled=ready && policy.editable(cue),
                        modifier=Modifier.weight(1f)) {
                        Text("Aplicar tempos")
                    }
                }
            }
        }
    }
}
