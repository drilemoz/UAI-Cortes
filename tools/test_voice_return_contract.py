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
assert 'GeminiVoiceChoice.visibleChoice(filtered, draftVoiceId)' in section
assert 'if (chosen != null)' in section
for token in ['selectedVoice = chosen','voicePicker = false','status = "Voz escolhida:']:
    assert token in section,token
for forbidden in ['api.synthesize','api.generate','AudioTrack(','update(project','loadVoices()']:
    assert forbidden not in section,forbidden
assert 'Modifier.fillMaxWidth().clickable {' in voice and 'draftVoiceId = voice.id' in voice
assert 'RadioButton(selected = draftVoiceId == voice.id,' in voice and 'onClick = {' in voice
assert 'val narrationDraft = remember(project?.id) { GeminiNarrationDraft() }' in main
assert 'narrationDraft = narrationDraft' in main and 'onSaveGeminiToAudio = { changed ->' in main
assert 'draft=narrationDraft ?: remember(p.id)' in audio
assert 'onSaveToAudio = { changed ->' in audio
assert 'autoPlayOnReturn = resumeVideoPreview' in main
assert 'seekProjectPosition(player, p.clips, latestPosition)' in editor
assert editor.index('seekProjectPosition(player, p.clips, latestPosition)') < editor.index('if (autoPlayOnReturn)') < editor.index('player.play()',editor.index('if (autoPlayOnReturn)'))
assert 'catalog.firstOrNull { it.id == draftId }' in choice
assert 'voicePicker = false' in voice and 'if (voicePicker && expanded) Dialog(onDismissRequest = {' in voice
print('OK: escolha de voz volta ao gerador; seleção não chama síntese nem altera projeto')
