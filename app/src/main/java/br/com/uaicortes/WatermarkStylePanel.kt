package br.com.uaicortes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val watermarkPalette = listOf(
    "Branco" to 0xFFFFFF, "Cinza-claro" to 0xABB4C0, "Cinza" to 0x65717F,
    "Chumbo" to 0x303944, "Preto" to 0x000000,
    "Amarelo" to 0xFFDE42, "Laranja" to 0xFF8C38, "Coral" to 0xFF6C63,
    "Vermelho" to 0xF13044, "Rosa" to 0xEB6DC2,
    "Verde-claro" to 0xB3E556, "Verde" to 0x40CE85, "Ciano" to 0x47DFEF,
    "Azul" to 0x3E83F8, "Roxo" to 0xA878F1
)

@Composable
internal fun WatermarkStylePanel(draft: TextWatermark, onUpdate: (TextWatermark) -> Unit,
    savedPresets: WatermarkDefaultsStore) {
    var open by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    val fonts = SubtitleStyles.watermarkFonts
    val fontLabel = fonts.firstOrNull { it.first == draft.fontFamily }?.second ?: "Moderna"
    val hex = WatermarkColor.hex(draft.color)
    fun toggle(value: String) { open = if (open == value) null else value }

    LayerOptionRow("Cor", "#$hex", open == "color") { toggle("color") }
    if (open == "color") WatermarkPalette(draft.color) { onUpdate(draft.copy(color = it)) }
    LayerOptionRow("Fonte", fontLabel, open == "font") { toggle("font") }
    if (open == "font") {
        val sample = draft.text.trim().ifBlank { "Minha marca d'água" }
        fonts.forEach { (family, label) ->
            val selected = draft.fontFamily == family
            Surface(onClick = { onUpdate(draft.copy(fontFamily = family)) },
                modifier = Modifier.fillMaxWidth(), shape = UaiShapes.item,
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                        Text(sample, fontFamily = subtitleFont(family, bold = true),
                            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (selected) Icon(Icons.Default.Check, "Fonte selecionada")
                }
            }
        }
    }
    LayerOptionRow("Tamanho", draft.sizeSp.toString(), open == "size") { toggle("size") }
    if (open == "size") WatermarkStepSlider("Tamanho", draft.sizeSp, 12, 36) {
        onUpdate(draft.copy(sizeSp = it))
    }
    LayerOptionRow("Opacidade", "${(draft.opacity * 100).toInt()}%", open == "opacity") { toggle("opacity") }
    if (open == "opacity") WatermarkStepSlider("Opacidade (%)", (draft.opacity * 100).toInt(), 0, 100) {
        onUpdate(draft.copy(opacity = it / 100f))
    }
    LayerOptionRow("Contorno e sombra", open = open == "effects") { toggle("effects") }
    if (open == "effects") {
        WatermarkStepSlider("Contorno", draft.outlineWidth, 0, 4) { onUpdate(draft.copy(outlineWidth = it)) }
        WatermarkStepSlider("Sombra", draft.shadowOffsetDp, 0, 6) { onUpdate(draft.copy(shadowOffsetDp = it)) }
    }
    LayerOptionRow("Padrão", open = open == "defaults") { toggle("defaults") }
    if (open == "defaults") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val preset = savedPresets.load()
                message = if (preset == null) "Nenhum padrão salvo neste aparelho." else {
                    onUpdate(preset.copy(clipId = draft.clipId))
                    "Padrão carregado."
                }
            }, modifier = Modifier.weight(1f)) { Text("Usar padrão") }
            OutlinedButton(onClick = {
                message = if (savedPresets.save(draft)) "Padrão salvo."
                    else "Não foi possível salvar o padrão."
            }, modifier = Modifier.weight(1f)) { Text("Salvar padrão") }
        }
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WatermarkPalette(color: Int, onSelect: (Int) -> Unit) {
    var showHex by remember { mutableStateOf(false) }
    var input by remember(color) { mutableStateOf(WatermarkColor.hex(color)) }
    watermarkPalette.chunked(5).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            row.forEach { (name, rgb) ->
                val argb = 0xFF000000.toInt() or rgb
                val selected = color == argb
                val swatch = Color(argb)
                Surface(onClick = { onSelect(argb) }, color = swatch, shape = CircleShape,
                    modifier = Modifier.size(44.dp).semantics {
                        contentDescription = if (selected) "$name, selecionada" else name
                    }, border = BorderStroke(if (selected) 2.dp else 1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (selected) Icon(Icons.Default.Check, null, Modifier.size(20.dp),
                            tint = if (swatch.luminance() > .4f) Color.Black else Color.White)
                    }
                }
            }
        }
    }
    TextButton(onClick = { showHex = !showHex }) { Text("Cor personalizada") }
    if (showHex) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val parsed = WatermarkColor.parse(input)
        OutlinedTextField(input, { input = it.take(7) }, singleLine = true,
            label = { Text("Hexadecimal") }, modifier = Modifier.weight(1f))
        Button(onClick = { parsed?.let(onSelect) }, enabled = parsed != null) { Text("Usar") }
    }
}
