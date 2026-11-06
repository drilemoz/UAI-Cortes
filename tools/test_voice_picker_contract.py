#!/usr/bin/env python3
"""A seleção de voz de narração é local; o mesmo componente é usado pelo editor."""
from pathlib import Path
src=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/uaicortes'
ui=(src/'GeminiNarrationUi.kt').read_text()
center=(src/'GeminiVoiceCenterScreen.kt').read_text()
presentation=(src/'GeminiVoicePresentation.kt').read_text()
assert 'if (expanded && !voicePicker && !referenceDialog && compareCueId == null) Dialog(' in ui
assert 'if (voicePicker && expanded) Dialog(' in ui
section=ui[ui.index('    // Catálogo e prévias centralizados:'):ui.index('    if (reuseDialogOpen && expanded) {')]
for token in ('GeminiVoiceCenterScreen(', 'selectedVoice = chosen', 'voicePicker = false', 'status = "Voz escolhida:', 'generated = null'):
    assert token in section, token
for forbidden in ('api.synthesize', 'onSaveToAudio(', 'AudioTrack(', 'loadVoices()'):
    assert forbidden not in section, forbidden
assert 'GeminiVoiceSearch.byGender(' in center
assert 'GeminiVoicePresentation.shortDetails(voice)' in center
assert 'Características não informadas' in presentation
print('OK: narração usa Central, seleção não dispara síntese nem altera projeto')
