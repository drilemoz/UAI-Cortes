package br.com.uaicortes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

/** Miniaturas geradas pela mesma definição visual persistida no projeto. */
@Composable
internal fun SubtitleStyleGallery(current: SubtitleCue, onChange: (SubtitleCue) -> Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        SubtitleStyles.presets.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                pair.forEach { preset ->
                    val selected=SubtitleStyles.matches(current,preset)
                    Box(Modifier.weight(1f)
                        .border(if(selected) 2.dp else 1.dp,
                            if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(10.dp))
                        .clickable { onChange(SubtitleStyles.applyToCue(current,preset)) }
                        .padding(5.dp)) {
                        Column(horizontalAlignment=Alignment.CenterHorizontally,
                            verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.fillMaxWidth().height(76.dp)
                                .background(Color(0xFF222428),RoundedCornerShape(6.dp)),
                                contentAlignment=Alignment.Center) {
                                val example=SubtitleCue(text="UAI INCRÍVEL",startMs=0,endMs=1000,
                                    color=preset.foreground,backgroundColor=preset.background,
                                    bold=preset.bold,italic=preset.italic,fontFamily=preset.fontFamily,
                                    outlineWidth=preset.outlineWidth,outlineColor=preset.outlineColor,
                                    shadowOffsetDp=preset.shadowOffsetDp ?: 0,
                                    highlightColor=preset.highlightColor,
                                    wordColors=listOf(SubtitleWordColor(1,preset.highlightColor)))
                                SubtitleStyledSample(example,SubtitleAnimations.Frame(example.text),compact=true)
                            }
                            Text(preset.name,style=MaterialTheme.typography.labelSmall,
                                maxLines=1,textAlign=TextAlign.Center)
                        }
                    }
                }
                if(pair.size==1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Mostra o contorno sob o preenchimento, inclusive nas palavras coloridas. */
@Composable
internal fun SubtitleStyledSample(cue: SubtitleCue, frame: SubtitleAnimations.Frame,
    compact: Boolean = false) {
    val shown=frame.text.ifBlank { "Prévia da legenda" }
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
    Text("Toque na palavra e escolha uma cor. A cor fica fixa; o karaokê destaca a palavra falada.",
        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
        ranges.chunked(3).forEachIndexed { row, chunk ->
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.fillMaxWidth()) {
                chunk.forEachIndexed { index,range ->
                    val actual=row*3+index
                    val color=draft.wordColors.firstOrNull {it.index==actual}?.color ?: draft.color
                    FilterChip(selected=selectedIndex==actual,onClick={selectedIndex=actual},
                        label={Text(draft.text.substring(range),fontSize=11.sp,maxLines=1,
                            color=if(draft.wordColors.any{it.index==actual}) Color(color)
                                else Color.Unspecified)},modifier=Modifier.weight(1f))
                }
                repeat(3-chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    if(ranges.isNotEmpty()) {
        Text("Cor da palavra selecionada",style=MaterialTheme.typography.labelMedium)
        palette.chunked(3).forEach { row ->
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.fillMaxWidth()) {
                row.forEach { (name,color) ->
                    OutlinedButton(onClick={
                        onChange(draft.copy(wordColors=draft.wordColors.filterNot{it.index==selectedIndex} +
                            SubtitleWordColor(selectedIndex,color)))
                    },modifier=Modifier.weight(1f),contentPadding=PaddingValues(3.dp)) {
                        Text(name,color=MaterialTheme.colorScheme.onSurface,fontSize=10.sp)
                    }
                }
            }
        }
        TextButton(onClick={onChange(draft.copy(wordColors=draft.wordColors.filterNot{
            it.index==selectedIndex }))}) { Text("Restaurar cor da palavra") }
    }
    HorizontalDivider()
    Text("Cor do destaque do karaokê",fontWeight=FontWeight.SemiBold)
    palette.chunked(3).forEach { row ->
        Row(horizontalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.fillMaxWidth()) {
            row.forEach { (name,color) ->
                FilterChip(selected=draft.highlightColor==color,
                    onClick={onChange(draft.copy(highlightColor=color))},
                    label={Text(name,fontSize=10.sp)},modifier=Modifier.weight(1f))
            }
        }
    }
    if(draft.wordTimings.isNotEmpty()) Text("Sincronia real: ${draft.wordTimings.size} palavras reconhecidas.",
        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    else Text("Sem horários por palavra: o karaokê usa distribuição aproximada no intervalo.",
        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
}
