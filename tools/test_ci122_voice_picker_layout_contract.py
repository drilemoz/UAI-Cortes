#!/usr/bin/env python3
"""Seletor v0.1.145; contratos estáticos, não substitui testes de layout Android."""
from pathlib import Path
ui=(Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/uaicortes/VoiceIaScreen.kt').read_text()
start=ui.index('    if (voicePickerCueId != null || voicePickerForAll) {')
end=ui.index('    if (stylePickerCueId != null || stylePickerForAll) {',start)
popup=ui[start:end]
assert 'DialogProperties(usePlatformDefaultWidth = false)' in popup
assert 'fillMaxWidth(0.96f).widthIn(max = 600.dp)' in popup
assert 'fillMaxHeight(0.83f)' in popup
assert 'horizontalScroll(' not in popup
assert 'VoiceIaPickerFilter(' in popup
assert 'Modifier.weight(1f)' in popup
assert '"Não informado" to "N/D"' in popup
assert '"Femininas", "Masculinas"' in popup
assert '"Graves" to "Grave"' in popup and '"Agudas" to "Aguda"' in popup
assert 'LazyColumn(modifier = Modifier.weight(1f)' in popup
assert 'Limpar filtros' in popup and 'Limpar pesquisa' in popup
assert 'currentId == voice.id' in popup and 'Voz selecionada' in popup
assert 'pendingVoiceForAll = voice.id' in popup
assert 'voiceOverrides[targetId] = voice.id' in popup
assert 'comparator.compare(firstParts.groupValues[1], secondParts.groupValues[1])' in popup
assert 'api.synthesize(' in ui # o seletor não muda a geração
print('OK: janela ampla, filtros sem overflow, vozes ordenadas e seleção preservada')
