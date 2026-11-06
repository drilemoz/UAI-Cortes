#!/usr/bin/env python3
"""Regressão da barra v190: quatro ações, um único seletor compartilhado."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
ui = (root/'app/src/main/java/br/com/uaicortes/VoiceIaScreen.kt').read_text()
center = (root/'app/src/main/java/br/com/uaicortes/GeminiVoiceCenterScreen.kt').read_text()
bar = ui[ui.index('// A Central guarda catálogo, modelo, voz e prévias;'):ui.index('savedQueue?.takeIf { it.unfinished }')]
assert bar.count('VoiceIaToolbarAction(') == 4
for action in ('label = "Central"', 'label = "Tom"', 'label = "Gerar $pending"', 'label = "Inserir $preparedCount"'):
    assert action in bar
assert 'voicePickerForAll = true' in bar
assert 'stylePickerCueId = selectedCue.id' in bar
assert 'Button(' not in bar and 'OutlinedButton(' not in bar
assert 'Surface(onClick = onClick, enabled = enabled' in ui
assert 'semantics { contentDescription = accessibilityLabel }' in ui
assert 'GeminiVoiceCenterScreen(' in ui
assert 'GeminiNarrationStyling.presets.sortedWith' in ui
assert 'api.synthesize(key, usedModel, voiceId, requestStyle, cue.text)' in ui
assert 'GeminiVoiceSearch.byGender(' in center and 'GeminiVoiceSearch.byPitch(' in center
assert 'LazyColumn(' in center
assert 'SubtitleOverlayOnFrame(selected, editable = false' in ui
print('OK: barra Central/Tom/Gerar/Inserir, seletor compartilhado e síntese separada')
