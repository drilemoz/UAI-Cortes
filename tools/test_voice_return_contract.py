#!/usr/bin/env python3
"""Contrato estático da navegação de voz. Não substitui teste UI Android."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/br/com/uaicortes'
voice=(BASE/'GeminiNarrationUi.kt').read_text()
audio=(BASE/'AudioScreen.kt').read_text()
main=(BASE/'MainActivity.kt').read_text()
editor=(BASE/'EditorScreen.kt').read_text()
choice=(BASE/'GeminiVoiceChoice.kt').read_text()
section=voice.split('Text("Confirmar voz")')[0].rsplit('Button(onClick = {',1)[-1]
assert 'GeminiVoiceChoice.confirmed(voices, draftVoiceId)' in section
assert 'if (chosen != null)' in section
for token in ['selectedVoice = chosen','voicePicker = false','expanded = false','onReturnToVideo()']:
    assert token in section,token
for forbidden in ['api.synthesize','api.generate','AudioTrack(','update(project','loadVoices()']:
    assert forbidden not in section,forbidden
assert '.clickable { draftVoiceId = voice.id }' in voice
assert 'RadioButton(selected = draftVoiceId == voice.id, onClick = null' in voice
assert 'val narrationDraft = remember(project?.id) { GeminiNarrationDraft() }' in main
assert 'narrationDraft = narrationDraft' in main and 'onReturnToVideo = { resumeVideoPreview = true; screen = "editor" }' in main
assert 'draft=narrationDraft ?: remember(p.id)' in audio
assert 'onReturnToVideo=onReturnToVideo' in audio
assert 'autoPlayOnReturn = resumeVideoPreview' in main
assert 'seekProjectPosition(player, p.clips, latestPosition)' in editor
assert editor.index('seekProjectPosition(player, p.clips, latestPosition)') < editor.index('if (autoPlayOnReturn)') < editor.index('player.play()',editor.index('if (autoPlayOnReturn)'))
assert 'catalog.firstOrNull { it.id == draftId }' in choice
assert 'voicePicker = false' in voice and 'onDismissRequest = { voicePicker = false }' in voice
print('OK: seleção confirmada, retorno, estado do projeto e retomada após cursor — sem síntese automática')
