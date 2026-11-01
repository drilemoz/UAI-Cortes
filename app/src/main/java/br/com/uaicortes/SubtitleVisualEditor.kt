package br.com.uaicortes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/** Alterações visuais em duas abas; nada nesta UI altera o texto ou os tempos. */
@Composable
internal fun SubtitleVisualControls(draft: SubtitleCue, onChange: (SubtitleCue)->Unit) {
    var tab by remember { mutableStateOf(0) }
    var advanced by remember { mutableStateOf(false) }
    var sampleMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(draft.animation,draft.animationSpeed,draft.text) {
        sampleMs=0L
        while(true) { delay(90L); sampleMs=(sampleMs+90L)%3000L }
    }
    val sample=SubtitleAnimations.frame(draft.copy(startMs=0L,endMs=3000L),sampleMs)
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min=76.dp)
            .background(Color(0xFF13151A),RoundedCornerShape(10.dp)).padding(12.dp),
            contentAlignment=Alignment.Center) {
            Box(Modifier.graphicsLayer {
                alpha=sample.alpha
                scaleX=sample.scale;scaleY=sample.scale
                translationY=sample.slideY*110f
            }.background(Color(draft.backgroundColor),RoundedCornerShape(3.dp))
                .padding(horizontal=7.dp,vertical=3.dp)) {
                Text(sample.text.ifBlank { "Prévia da legenda" },color=Color(draft.color),
                    fontSize=draft.sizeSp.sp,fontFamily=subtitleFont(draft.fontFamily),
                    fontWeight=if(draft.bold) FontWeight.Bold else FontWeight.Normal,
                    textAlign=TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("Estilo","Animação").forEachIndexed { i,name ->
                if (tab==i) Button(onClick={tab=i},modifier=Modifier.weight(1f),
                    contentPadding=PaddingValues(vertical=9.dp)) {Text(name)}
                else OutlinedButton(onClick={tab=i},modifier=Modifier.weight(1f),
                    contentPadding=PaddingValues(vertical=9.dp)) {Text(name)}
            }
        }
        if(tab==0) {
            Text("Estilos rápidos",fontWeight=FontWeight.SemiBold)
            SubtitleTwoColumnChoices(SubtitleStyles.presets.map { it.name },
                selected={ name -> SubtitleStyles.presets.firstOrNull { it.name==name }?.let {
                    draft.color==it.foreground && draft.backgroundColor==it.background
                } ?: false },
                choose={ name -> SubtitleStyles.presets.firstOrNull { it.name==name }?.let {
                    onChange(SubtitleStyles.applyToCue(draft,it))
                } })
            SubtitleValueSlider("Tamanho", "${draft.sizeSp} sp",draft.sizeSp.toFloat(),12f..48f,
                onChange={onChange(draft.copy(sizeSp=it.toInt().coerceIn(12,48)))})
            OutlinedButton(onClick={advanced=!advanced},modifier=Modifier.fillMaxWidth()) {
                Text(if(advanced) "Ocultar ajustes avançados ▲" else "Ajustes avançados ▼")
            }
            if(advanced) {
                Text("Fonte",fontWeight=FontWeight.Medium)
                SubtitleTwoColumnChoices(SubtitleStyles.fonts.map { it.second },
                    selected={ name -> SubtitleStyles.fonts.any { it.second==name && it.first==draft.fontFamily } },
                    choose={name-> SubtitleStyles.fonts.firstOrNull {it.second==name}?.let {
                        onChange(draft.copy(fontFamily=it.first))
                    }})
                SubtitleValueSlider("Contorno", "${draft.outlineWidth} dp",draft.outlineWidth.toFloat(),0f..4f,
                    3,onChange={onChange(draft.copy(outlineWidth=it.toInt()))})
                if(draft.outlineWidth>0) SubtitleTwoColumnChoices(listOf("Preto","Branco"),
                    selected={ it=="Branco" && draft.outlineColor==-1 || it=="Preto" && draft.outlineColor!=-1 },
                    choose={onChange(draft.copy(outlineColor=if(it=="Branco") -1 else 0xFF000000.toInt()))})
                SubtitleValueSlider("Sombra", "${draft.shadowOffsetDp} dp",draft.shadowOffsetDp.toFloat(),0f..6f,
                    5,onChange={onChange(draft.copy(shadowOffsetDp=it.toInt()))})
                if(draft.shadowOffsetDp>0) SubtitleTwoColumnChoices(listOf("Preta","Branca"),
                    selected={ it=="Branca" && draft.shadowColor==-1 || it=="Preta" && draft.shadowColor!=-1 },
                    choose={onChange(draft.copy(shadowColor=if(it=="Branca") -1 else 0xFF000000.toInt()))})
                SubtitleValueSlider("Espaçamento", "${draft.lineSpacingPercent}%",draft.lineSpacingPercent.toFloat(),100f..160f,
                    5,onChange={onChange(draft.copy(lineSpacingPercent=it.toInt()))})
                Text("Alinhamento",fontWeight=FontWeight.Medium)
                SubtitleTwoColumnChoices(SubtitleStyles.alignments.map{it.second},
                    selected={label->SubtitleStyles.alignments.any{it.second==label && it.first==draft.textAlignment}},
                    choose={label->SubtitleStyles.alignments.firstOrNull{it.second==label}?.let{
                        onChange(draft.copy(textAlignment=it.first))
                    }})
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Negrito",modifier=Modifier.weight(1f))
                    Switch(checked=draft.bold,onCheckedChange={onChange(draft.copy(bold=it))})
                }
                SubtitleValueSlider("Posição horizontal", "${(draft.x*100).toInt()}%",draft.x,.1f.. .9f,
                    onChange={onChange(draft.copy(x=it))})
                SubtitleValueSlider("Posição vertical", "${(draft.y*100).toInt()}%",draft.y,.1f.. .9f,
                    onChange={onChange(draft.copy(y=it))})
                SubtitleTwoColumnChoices(listOf("Cima","Centro","Baixo"),
                    selected={label->kotlin.math.abs(draft.y-when(label){"Cima"->.15f;"Centro"->.5f;else->.85f})<.02f},
                    choose={label->onChange(draft.copy(y=when(label){"Cima"->.15f;"Centro"->.5f;else->.85f}))})
            }
        } else {
            Text("Efeitos de entrada e revelação",fontWeight=FontWeight.SemiBold)
            SubtitleTwoColumnChoices(SubtitleAnimations.choices.map{it.second},
                selected={label->SubtitleAnimations.choices.any{it.second==label && draft.animation==it.first}},
                choose={label->SubtitleAnimations.choices.firstOrNull{it.second==label}?.let{
                    onChange(draft.copy(animation=it.first))
                }})
            if(draft.animation!=SubtitleAnimations.NONE) {
                SubtitleValueSlider("Velocidade", "${"%.1f".format(java.util.Locale.US,draft.animationSpeed)}×",
                    draft.animationSpeed,.5f..2f,5,onChange={onChange(draft.copy(animationSpeed=it))})
                Text("A animação segue o intervalo da legenda, sem sincronização palavra a palavra com o áudio.",
                    color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SubtitleTwoColumnChoices(options: List<String>,selected:(String)->Boolean,
    choose:(String)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
        options.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                pair.forEach { option ->
                    FilterChip(selected=selected(option),onClick={choose(option)},
                        label={Text(option,maxLines=2,fontSize=12.sp)},
                        modifier=Modifier.weight(1f))
                }
                if(pair.size==1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Slider com margens para não recortar o polegar na borda do modal. */
@Composable
private fun SubtitleValueSlider(label:String, valueLabel:String, value:Float,
    range: ClosedFloatingPointRange<Float>,steps:Int=0,onChange:(Float)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(0.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(label,style=MaterialTheme.typography.bodySmall)
            Text(valueLabel,style=MaterialTheme.typography.labelSmall)
        }
        Slider(value=value.coerceIn(range.start,range.endInclusive),valueRange=range,
            steps=steps,onValueChange=onChange,
            modifier=Modifier.fillMaxWidth().padding(horizontal=12.dp))
    }
}

/** Edição em lote com confirmação, sem modificar textos, IDs ou horários. */
@Composable
internal fun SubtitleBatchEditDialog(source:SubtitleCue,count:Int,onDismiss:()->Unit,
    onApply:(SubtitleCue,SubtitleStyles.CopyOptions)->Unit) {
    var draft by remember(source.id) { mutableStateOf(source) }
    var applyStyle by remember { mutableStateOf(true) }
    var applyAnimation by remember { mutableStateOf(true) }
    var applyPosition by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth(.96f)
            .heightIn(max=LocalConfiguration.current.screenHeightDp.dp*.86f),
            shape=RoundedCornerShape(18.dp),tonalElevation=5.dp) {
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Editar todas ($count)",fontWeight=FontWeight.Bold,
                        style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                    IconButton(onClick=onDismiss) {Icon(Icons.Default.Close,"Fechar")}
                }
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    SubtitleVisualControls(draft,onChange={draft=it})
                    HorizontalDivider()
                    Text("O que aplicar às $count legendas?",fontWeight=FontWeight.SemiBold)
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(applyStyle,onCheckedChange={applyStyle=it})
                        Text("Estilo (fonte, cores, tamanho e alinhamento)",fontSize=12.sp)
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(applyAnimation,onCheckedChange={applyAnimation=it})
                        Text("Animação e velocidade",fontSize=12.sp)
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(applyPosition,onCheckedChange={applyPosition=it})
                        Text("Posição na imagem",fontSize=12.sp)
                    }
                    Text("Texto, início, fim e identificação de cada legenda serão preservados.",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick=onDismiss,modifier=Modifier.weight(1f)) {Text("Cancelar")}
                    Button(onClick={confirm=true},enabled=applyStyle||applyAnimation||applyPosition,
                        modifier=Modifier.weight(1f)) {Text("Aplicar a todas")}
                }
            }
        }
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},
        title={Text("Alterar $count legendas?")},
        text={Text("Os estilos, animações e/ou posições selecionados serão substituídos nas $count legendas. Textos e tempos não mudam. Será possível desfazer após aplicar.")},
        confirmButton={TextButton(onClick={
            confirm=false
            onApply(draft,SubtitleStyles.CopyOptions(appearance=applyStyle,
                position=applyPosition,animation=applyAnimation))
        }) {Text("Aplicar")}},
        dismissButton={TextButton(onClick={confirm=false}) {Text("Cancelar")}})
}
