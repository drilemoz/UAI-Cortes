package br.com.uaicortes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Galeria horizontal FINITA: do primeiro ao último preset, sem repetir itens. */
@Composable
internal fun SubtitleStyleGallery(current: SubtitleCue, onChange: (SubtitleCue) -> Unit) {
    var sampleMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) { delay(80L); sampleMs = (sampleMs + 80L) % 2500L }
    }
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("Deslize para os lados · ${SubtitleStyles.presets.size} modelos de legendas",
            style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(9.dp),
            contentPadding=PaddingValues(end=12.dp)) {
            itemsIndexed(SubtitleStyles.presets, key={ index,preset -> "${index}:${preset.name}" }) { _,preset ->
                val selected=SubtitleStyles.matches(current,preset)
                val example=remember(preset) {
                    SubtitleCue(text="UAI INCRÍVEL",startMs=0,endMs=2500,
                        color=preset.foreground,backgroundColor=preset.background,
                        bold=preset.bold,italic=preset.italic,fontFamily=preset.fontFamily,
                        outlineWidth=preset.outlineWidth,outlineColor=preset.outlineColor,
                        shadowOffsetDp=preset.shadowOffsetDp ?: 0,
                        highlightColor=preset.highlightColor,
                        animation=preset.animation ?: SubtitleAnimations.NONE,
                        wordColors=listOf(SubtitleWordColor(1,preset.highlightColor)))
                }
                val frame=SubtitleAnimations.frame(example,sampleMs)
                Surface(onClick={onChange(SubtitleStyles.applyToCue(current,preset))},
                    modifier=Modifier.width(142.dp),
                    shape=RoundedCornerShape(12.dp),
                    color=if(selected)MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    border=BorderStroke(if(selected)2.dp else 1.dp,
                        if(selected)MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Box(Modifier.fillMaxWidth().height(64.dp)
                            .background(Color(0xFF222428),RoundedCornerShape(7.dp)),
                            contentAlignment=Alignment.Center) {
                            Box(Modifier.graphicsLayer {
                                alpha=frame.alpha
                                scaleX=frame.scale;scaleY=frame.scale
                                translationY=frame.slideY*65f
                                translationX=frame.slideX*142f
                            }) { SubtitleStyledSample(example,frame,compact=true) }
                        }
                        Text(preset.name,style=MaterialTheme.typography.labelMedium,
                            maxLines=1,textAlign=TextAlign.Center)
                        Text(SubtitleAnimations.choices.firstOrNull { it.first==preset.animation }?.second
                            ?: "Sem efeito",maxLines=1,
                            style=MaterialTheme.typography.labelSmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Mostra o contorno sob o preenchimento, inclusive nas palavras coloridas. */
@Composable
internal fun SubtitleStyledSample(cue: SubtitleCue, frame: SubtitleAnimations.Frame,
    compact: Boolean = false) {
    // Não inventar texto quando o efeito (ex.: máquina de escrever) ainda está vazio.
    if (frame.text.isBlank()) return
    val shown=frame.text
    val colored=buildAnnotatedString {
        append(shown)
        SubtitleWordEffects.colors(cue,shown,frame.highlighted).forEach { span ->
            addStyle(SpanStyle(color=Color(span.color)),span.range.first,span.range.last+1)
        }
    }
    val weight=if(cue.bold) FontWeight.Bold else FontWeight.Normal
    val style=if(cue.italic) FontStyle.Italic else FontStyle.Normal
    val fontSize=if(compact) 16.sp else cue.sizeSp.coerceIn(12,38).sp
    Box(Modifier.background(Color(cue.backgroundColor),RoundedCornerShape(4.dp))
        .padding(horizontal=if(compact) 4.dp else 7.dp,vertical=3.dp),
        contentAlignment=Alignment.Center) {
        if(cue.outlineWidth>0) {
            Text(shown,color=Color(cue.outlineColor),fontSize=fontSize,
                fontWeight=weight,fontStyle=style,fontFamily=subtitleFont(cue.fontFamily),
                maxLines=3,textAlign=TextAlign.Center,
                style=TextStyle(drawStyle=Stroke(width=(if(compact) cue.outlineWidth*.9f else cue.outlineWidth*2f),
                    join=StrokeJoin.Round)))
        }
        Text(colored,color=Color(cue.color),fontSize=fontSize,fontWeight=weight,
            fontStyle=style,fontFamily=subtitleFont(cue.fontFamily),maxLines=3,
            textAlign=TextAlign.Center,
            style=TextStyle(shadow=if(cue.shadowOffsetDp>0)
                androidx.compose.ui.graphics.Shadow(
                    color=Color(cue.shadowColor),
                    offset=androidx.compose.ui.geometry.Offset(
                        cue.shadowOffsetDp.toFloat(),cue.shadowOffsetDp.toFloat()),
                    blurRadius=0f) else null))
    }
}

/** Edita uma palavra por índice, de modo que repetições tenham cores independentes. */
@Composable
internal fun SubtitleWordColorEditor(draft: SubtitleCue, onChange:(SubtitleCue)->Unit) {
    val ranges=remember(draft.text) { SubtitleWordEffects.wordRanges(draft.text).take(30) }
    var selectedIndex by remember(draft.id,draft.text) { mutableIntStateOf(0) }
    val palette=listOf(
        "Branco" to 0xFFFFFFFF.toInt(),
        "Amarelo" to 0xFFFFFF00.toInt(),
        "Azul" to 0xFF00CCFF.toInt(),
        "Vermelho" to 0xFFFF2828.toInt(),
        "Roxo" to 0xFFD549FF.toInt(),
        "Verde" to 0xFF65FF60.toInt()
    )
    Text("Cores por palavra",fontWeight=FontWeight.SemiBold)
    Text("Selecione a palavra e depois a cor.",
        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp),
        contentPadding=PaddingValues(end=12.dp)) {
        items(ranges.size) { index ->
            val range=ranges[index]
            val color=draft.wordColors.firstOrNull { it.index==index }?.color ?: draft.color
            FilterChip(selected=selectedIndex==index,onClick={selectedIndex=index},
                label={Text(draft.text.substring(range),maxLines=1,fontSize=12.sp,
                    color=if(draft.wordColors.any {it.index==index})Color(color)
                        else Color.Unspecified)},shape=RoundedCornerShape(9.dp))
        }
    }
    if(ranges.isNotEmpty()) {
        Text("Cor da palavra selecionada",style=MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp),
            contentPadding=PaddingValues(end=12.dp)) {
            items(palette.size) { index ->
                val (name,color)=palette[index]
                FilterChip(selected=draft.wordColors.any {it.index==selectedIndex && it.color==color},
                    onClick={onChange(draft.copy(wordColors=
                        draft.wordColors.filterNot{it.index==selectedIndex}+
                        SubtitleWordColor(selectedIndex,color)))},
                    label={Text(name,fontSize=12.sp)},shape=RoundedCornerShape(9.dp))
            }
        }
        TextButton(onClick={onChange(draft.copy(wordColors=draft.wordColors.filterNot{
            it.index==selectedIndex }))}) { Text("Restaurar cor da palavra") }
    }
    HorizontalDivider()
    Text("Cor do destaque do karaokê",fontWeight=FontWeight.SemiBold)
    LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp),
        contentPadding=PaddingValues(end=12.dp)) {
        items(palette.size) { index ->
            val (name,color)=palette[index]
            FilterChip(selected=draft.highlightColor==color,
                onClick={onChange(draft.copy(highlightColor=color))},
                label={Text(name,fontSize=12.sp)},shape=RoundedCornerShape(9.dp))
        }
    }
    if(draft.wordTimings.isNotEmpty()) Text("Sincronia real: ${draft.wordTimings.size} palavras reconhecidas.",
        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    else Text("Sem horários por palavra: o karaokê usa distribuição aproximada no intervalo.",
        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
}
