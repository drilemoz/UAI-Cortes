#!/usr/bin/env python3
"""Regressão da barra v193: três ações e um seletor compartilhado."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
ui = (root/'app/src/main/java/br/com/uaicortes/VoiceIaScreen.kt').read_text()
center = (root/'app/src/main/java/br/com/uaicortes/GeminiVoiceCenterScreen.kt').read_text()
bar = ui[ui.index('// Visão principal: apenas progresso, três ações e lista de falas.'):ui.index('if (status.isNotBlank())')]
assert bar.count('VoiceIaToolbarAction(') == 3
for action in ('label = if (project.subtitleSpeakers.isEmpty()) "Vozes" else "Personagens"', 'label = "Gerar $pending"', 'label = "Inserir $preparedCount"'):
    assert action in bar
assert 'if (project.subtitleSpeakers.isEmpty()) voicePickerForAll = true' in bar
assert 'showSpeakerSettings = !showSpeakerSettings' in bar
assert 'stylePickerCueId = cue.id' in ui
assert 'OutlinedButton(' not in bar
assert 'Surface(onClick = onClick, enabled = enabled' in ui
assert 'semantics { contentDescription = accessibilityLabel }' in ui
assert 'GeminiVoiceCenterScreen(' in ui
assert 'GeminiNarrationStyling.presets.sortedWith' in ui
assert 'api.synthesize(key, usedModel, voiceId, requestStyle, cue.text)' in ui
assert 'GeminiVoiceSearch.byGender(' in center and 'GeminiVoiceSearch.byPitch(' in center
assert 'LazyColumn(' in center
assert 'SubtitleOverlayOnFrame(selected, editable = false' in ui
print('OK: barra Vozes/Gerar/Inserir, tom individual, seletor compartilhado e síntese separada')
