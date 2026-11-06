#!/usr/bin/env python3
"""Layout da Central compartilhada; testes visuais Android seguem necessários."""
from pathlib import Path
src=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/uaicortes'
ui=(src/'VoiceIaScreen.kt').read_text()
center=(src/'GeminiVoiceCenterScreen.kt').read_text()
start=ui.index('    if (voicePickerCueId != null || voicePickerForAll || voicePickerSpeakerId != null) {')
end=ui.index('    if (stylePickerCueId != null || stylePickerForAll || stylePickerSpeakerId != null) {',start)
popup=ui[start:end]
assert 'DialogProperties(usePlatformDefaultWidth = false)' in popup
assert 'fillMaxWidth(.97f).fillMaxHeight(.91f)' in popup
assert 'GeminiVoiceCenterScreen(' in popup
assert 'pendingVoiceForAll = voice.id' in popup
assert 'pendingSpeakerVoice = speakerId to voice.id' in popup
assert 'voiceOverrides[targetId] = voice.id' in popup
assert 'onChoose = { voice ->' in popup
assert 'Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())' in center
assert 'LazyColumn(Modifier.weight(1f)' in center
assert 'FilterChip(selected = gender == option' in center
assert 'FilterChip(selected = pitch == option' in center
assert 'onlyFavorites = !onlyFavorites' in center
assert 'selectedVoiceId' in center
print('OK: Central ampla com filtros roláveis, lista virtualizada e seleção preservada')
