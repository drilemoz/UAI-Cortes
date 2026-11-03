#!/usr/bin/env python3
"""Contrato estático do seletor compacto. Teste visual requer Android real."""
from pathlib import Path
base = Path(__file__).resolve().parents[1]
ui = (base/'app/src/main/java/br/com/uaicortes/GeminiNarrationUi.kt').read_text()
search = (base/'app/src/main/java/br/com/uaicortes/GeminiVoiceSearch.kt').read_text()
presentation = (base/'app/src/main/java/br/com/uaicortes/GeminiVoicePresentation.kt').read_text()
assert 'if (expanded && !voicePicker) Dialog(' in ui
assert 'if (voicePicker && expanded) Dialog(' in ui
assert 'GeminiVoiceSearch.byGender(searched, voiceGenderFilter)' in ui
assert 'GeminiVoicePresentation.shortDetails(voice)' in ui
assert 'expandedVoiceId = if (detailsOpen) null else voice.id' in ui
assert 'Descrição original da API' in ui and 'Confirmar voz' in ui
assert '"Femininas" -> voices.filter { it.gender.equals("female", true) }' in search
assert '"Masculinas" -> voices.filter { it.gender.equals("male", true) }' in search
assert 'Características não informadas' in presentation
section = ui.split('Text("Confirmar voz")')[0].rsplit('Button(onClick = {', 1)[-1]
for expected in ('GeminiVoiceChoice.visibleChoice(filtered, draftVoiceId)',
                 'selectedVoice = chosen', 'voicePicker = false', 'expanded = false',
                 'onReturnToVideo()'):
    assert expected in section, expected
for forbidden in ('api.synthesize', 'api.generate', 'AudioTrack(', 'update(project', 'loadVoices()'):
    assert forbidden not in section, forbidden
assert 'RadioButton(selected = draftVoiceId == voice.id,' in ui
assert 'draftVoiceId = GeminiVoiceChoice.visibleIdOrNull(visible, draftVoiceId)' in ui
assert 'val visibleChoice = GeminiVoiceChoice.visibleChoice(filtered, draftVoiceId)' in ui
assert 'enabled = visibleChoice != null && !working && !loading' in ui
assert 'pendingSampleVoiceId = candidate.id' in ui
assert 'GeminiVoiceChoice.confirmed(voices, pendingSampleVoiceId)' in ui
assert 'confirmation = "sample"' in ui
assert 'GeminiVoiceCompactPickerTest.kt'  in str(base/'app/src/test/java/br/com/uaicortes/GeminiVoiceCompactPickerTest.kt')
print('OK: seletor compacto, detalhes sob demanda, filtros sem inferência, sem sobreposição, retorno confirmado')
