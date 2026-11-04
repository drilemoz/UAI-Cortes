package br.com.uaicortes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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

/** Prévia sempre visível no topo do diálogo, independente da rolagem das opções. */
@Composable
internal fun SubtitleVisualPreview(draft: SubtitleCue) {
    var sampleMs by remember(draft.id) { mutableLongStateOf(0L) }
    var playing by remember(draft.id) { mutableStateOf(true) }
    var actualDuration by remember(draft.id) { mutableStateOf(false) }
    val previewDuration = if (actualDuration)
        (draft.endMs-draft.startMs).coerceAtLeast(1L) else 3000L
    LaunchedEffect(draft.id,draft.animation,draft.animationSpeed,draft.text,previewDuration) {
        sampleMs=0L
    }
    LaunchedEffect(draft.id,draft.animation,draft.animationSpeed,previewDuration,playing) {
        while (playing) {
            delay(90L)
            sampleMs=(sampleMs+90L)%previewDuration
        }
    }
    val sample=SubtitleAnimations.frame(
        SubtitleWordTimingEditorPolicy.previewCue(draft,previewDuration),sampleMs)
    val thumbnail=draft.copy(sizeSp=(12+(draft.sizeSp-12)/3).coerceIn(12,24))
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(83.dp)
            .background(Color(0xFF13151A),RoundedCornerShape(10.dp))
            .padding(horizontal=10.dp,vertical=4.dp),contentAlignment=Alignment.Center) {
            Box(Modifier.graphicsLayer {
                alpha=sample.alpha
                scaleX=sample.scale;scaleY=sample.scale
                translationY=sample.slideY*83f
                translationX=sample.slideX*320f
            }) { SubtitleStyledSample(thumbnail,sample) }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            TextButton(onClick={playing=!playing},contentPadding=PaddingValues(horizontal=6.dp)) {
                Text(if(playing) "Pausar" else "Reproduzir",fontSize=12.sp)
            }
            TextButton(onClick={sampleMs=0L},contentPadding=PaddingValues(horizontal=6.dp)) {
                Text("Reiniciar",fontSize=12.sp)
            }
            Spacer(Modifier.weight(1f))
            FilterChip(selected=actualDuration,onClick={actualDuration=!actualDuration},
                label={Text(if(actualDuration) "Tempo real" else "Demo 3s",fontSize=11.sp)})
        }
    }
}

/** Negrito/itálico/tamanho são controles básicos de toque único, não sliders longos. */
@Composable
internal fun SubtitleTypographyBar(draft: SubtitleCue, onChange:(SubtitleCue)->Unit) {
    Row(Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(9.dp))
        .padding(horizontal=7.dp,vertical=3.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        FilterChip(selected=draft.bold,onClick={onChange(draft.copy(bold=!draft.bold))},
            label={Text("B",fontWeight=FontWeight.Bold,fontSize=14.sp)},
            modifier=Modifier.semantics { contentDescription="Alternar negrito" },
            shape=RoundedCornerShape(8.dp))
        FilterChip(selected=draft.italic,onClick={onChange(draft.copy(italic=!draft.italic))},
            label={Text("I",fontStyle=FontStyle.Italic,fontSize=14.sp)},
            modifier=Modifier.semantics { contentDescription="Alternar itálico" },
            shape=RoundedCornerShape(8.dp))
        Spacer(Modifier.weight(1f))
        IconButton(onClick={onChange(draft.copy(sizeSp=(draft.sizeSp-2).coerceAtLeast(12)))},
            enabled=draft.sizeSp>12,modifier=Modifier.size(34.dp)) {
            Text("−",fontSize=21.sp)
        }
        Text("${draft.sizeSp} sp",style=MaterialTheme.typography.labelMedium,maxLines=1)
        IconButton(onClick={onChange(draft.copy(sizeSp=(draft.sizeSp+2).coerceAtMost(48)))},
            enabled=draft.sizeSp<48,modifier=Modifier.size(34.dp)) {
            Text("+",fontSize=21.sp)
        }
    }
}

/** Alterações visuais em duas abas; prévia fixa pertence ao diálogo pai. */
@Composable
internal fun SubtitleVisualControls(draft: SubtitleCue, onChange: (SubtitleCue)->Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var advanced by remember { mutableStateOf(false) }
    var wordColorsOpen by remember { mutableStateOf(false) }
    var positionOpen by remember { mutableStateOf(false) }
    var animationGroup by remember(draft.id) { mutableStateOf(
        SubtitleAnimations.groups.firstOrNull { draft.animation in it.second }?.first ?: "Entrada"
    ) }
    LaunchedEffect(draft.animation) {
        animationGroup = SubtitleAnimations.groups.firstOrNull { draft.animation in it.second }?.first ?: "Entrada"
    }
    Column(Modifier.fillMaxWidth().padding(horizontal=2.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            listOf("Estilo","Animação").forEachIndexed { i,name ->
                if (tab==i) Button(onClick={tab=i},shape=RoundedCornerShape(8.dp),
                    modifier=Modifier.weight(1f),contentPadding=PaddingValues(vertical=7.dp)) {
                    Text(name,fontSize=13.sp)
                } else OutlinedButton(onClick={tab=i},shape=RoundedCornerShape(8.dp),
                    modifier=Modifier.weight(1f),contentPadding=PaddingValues(vertical=7.dp)) {
                    Text(name,fontSize=13.sp)
                }
            }
        }
        if(tab==0) {
            SubtitleStyleGallery(draft,onChange)
            SubtitleUserPresetPanel(draft,onChange)
            SubtitleTypographyBar(draft,onChange)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                listOf(
                    Triple("Palavras",wordColorsOpen,{wordColorsOpen=!wordColorsOpen}),
                    Triple("Posição",positionOpen,{positionOpen=!positionOpen}),
                    Triple("Avançado",advanced,{advanced=!advanced})
                ).forEach { (label,open,toggle) ->
                    OutlinedButton(onClick=toggle,shape=RoundedCornerShape(8.dp),
                        modifier=Modifier.weight(1f),
                        contentPadding=PaddingValues(horizontal=2.dp,vertical=7.dp)) {
                        Text(label+(if(open) " ▲" else " ▼"),fontSize=11.sp,maxLines=1)
                    }
                }
            }
            if(wordColorsOpen) {
                SubtitleWordColorEditor(draft,onChange)
            }
            if(positionOpen) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Altura da legenda",fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f))
                    TextButton(onClick={onChange(draft.copy(y=.92f))}) { Text("Rodapé · 8%") }
                }
                SubtitleValueSlider("Vertical", "${(draft.y*100).toInt()}%",draft.y,.1f.. .95f,
                    onChange={onChange(draft.copy(y=it))})
                SubtitleValueSlider("Horizontal", "${(draft.x*100).toInt()}%",draft.x,.1f.. .9f,
                    onChange={onChange(draft.copy(x=it))})
            }
            if(advanced) {
                Text("Fonte",fontWeight=FontWeight.Medium)
                SubtitleTwoColumnChoices(SubtitleStyles.fonts.map { it.second },
                    selected={ name -> SubtitleStyles.fonts.any { it.second==name && it.first==draft.fontFamily } },
                    choose={name-> SubtitleStyles.fonts.firstOrNull {it.second==name}?.let {
                        onChange(draft.copy(fontFamily=it.first))
                    }})
                Text("Alinhamento",fontWeight=FontWeight.Medium)
                SubtitleTwoColumnChoices(SubtitleStyles.alignments.map{it.second},
                    selected={label->SubtitleStyles.alignments.any{it.second==label && it.first==draft.textAlignment}},
                    choose={label->SubtitleStyles.alignments.firstOrNull{it.second==label}?.let{
                        onChange(draft.copy(textAlignment=it.first))
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
            }
        } else {
            Text("Efeitos prontos · escolha uma categoria",fontWeight=FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                SubtitleAnimations.groups.forEach { group ->
                    FilterChip(selected=animationGroup==group.first,
                        onClick={animationGroup=group.first},
                        label={Text(group.first,fontSize=11.sp)},
                        modifier=Modifier.weight(1f))
                }
            }
            val ids=SubtitleAnimations.groups.firstOrNull { it.first==animationGroup }?.second.orEmpty()
            SubtitleTwoColumnChoices(ids.map(SubtitleAnimations::label),
                selected={label->draft.animation in ids && SubtitleAnimations.label(draft.animation)==label},
                choose={label-> ids.firstOrNull { SubtitleAnimations.label(it)==label }?.let { id ->
                    onChange(draft.copy(animation=id))
                }})
            Text("Use Reproduzir, Reiniciar e Tempo real para conferir a animação acima.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(draft.animation!=SubtitleAnimations.NONE) {
                SubtitleValueSlider("Velocidade", "${"%.1f".format(java.util.Locale.US,draft.animationSpeed)}×",
                    draft.animationSpeed,.5f..2f,5,onChange={onChange(draft.copy(animationSpeed=it))})
                Text(if(SubtitleAnimations.isKaraoke(draft.animation) && draft.wordTimings.isNotEmpty())
                    "Destaque usa os horários salvos de cada palavra."
                    else if (SubtitleAnimations.isKaraoke(draft.animation))
                    "Sem sincronização manual, o destaque divide o tempo igualmente (estimativa)."
                    else "A velocidade muda a animação; confira na prévia antes de exportar.",
                    color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Faixa de escolhas horizontal, limitada à primeira e última opção. */
@Composable
private fun SubtitleTwoColumnChoices(options: List<String>,selected:(String)->Boolean,
    choose:(String)->Unit) {
    LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp),
        contentPadding=PaddingValues(end=12.dp)) {
        items(options) { option ->
            FilterChip(selected=selected(option),onClick={choose(option)},
                label={Text(option,maxLines=1,fontSize=12.sp)},
                shape=RoundedCornerShape(9.dp))
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
    var applyWordColors by remember { mutableStateOf(false) }
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
                SubtitleVisualPreview(draft)
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    SubtitleVisualControls(draft,onChange={draft=it})
                    HorizontalDivider()
                    Text("Aplicar em $count legendas · escolha os grupos",fontWeight=FontWeight.SemiBold,
                        style=MaterialTheme.typography.bodyMedium)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp),
                        contentPadding=PaddingValues(end=12.dp)) {
                        item {
                            FilterChip(selected=applyStyle,onClick={applyStyle=!applyStyle},
                                label={Text("Estilo",fontSize=12.sp)},shape=RoundedCornerShape(8.dp))
                        }
                        item {
                            FilterChip(selected=applyAnimation,onClick={applyAnimation=!applyAnimation},
                                label={Text("Animação",fontSize=12.sp)},shape=RoundedCornerShape(8.dp))
                        }
                        item {
                            FilterChip(selected=applyPosition,onClick={applyPosition=!applyPosition},
                                label={Text("Posição",fontSize=12.sp)},shape=RoundedCornerShape(8.dp))
                        }
                    }
                    if (applyStyle) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Checkbox(checked=applyWordColors,onCheckedChange={applyWordColors=it})
                            Column {
                                Text("Copiar cores das mesmas palavras",fontSize=12.sp)
                                Text("Opcional: combina palavras iguais, não posições. " +
                                    "Outras cores são preservadas.",fontSize=11.sp,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (!applyStyle && !applyAnimation && !applyPosition) {
                        Text("Selecione pelo menos um grupo para aplicar.",fontSize=12.sp,
                            color=MaterialTheme.colorScheme.error)
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
        text={Text("${SubtitleStyles.CopyOptions(appearance=applyStyle, position=applyPosition, animation=applyAnimation, wordColors=applyWordColors).summary(count)}. Textos, horários e IDs não mudam. Você poderá desfazer.")},
        confirmButton={TextButton(onClick={
            confirm=false
            onApply(draft,SubtitleStyles.CopyOptions(appearance=applyStyle,
                position=applyPosition,animation=applyAnimation,
                wordColors=applyStyle && applyWordColors))
        }) {Text("Aplicar")}},
        dismissButton={TextButton(onClick={confirm=false}) {Text("Cancelar")}})
}
