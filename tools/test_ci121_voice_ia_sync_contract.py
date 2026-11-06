#!/usr/bin/env python3
"""Vozes IA v0.1.144: garante filtros e ajuste somente sob ação explícita."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
base = root/'app/src/main/java/br/com/uaicortes'
ui = (base/'VoiceIaScreen.kt').read_text()
policy = (base/'VoiceIaSync.kt').read_text()
draft = (base/'VoiceIaDraftStore.kt').read_text()
search = (base/'GeminiVoiceSearch.kt').read_text()
assert 'VoiceIaSync.review(' in ui and 'pendingSyncId = cue.id' in ui
assert 'adjustWav(cue, take, review.availableMs)' in ui
assert 'VoiceIaWavStretch.shorten(source.readBytes(), targetMs)' in ui
assert 'GeminiAudioFiles.save(context, wave)' in ui
assert 'sourceFilePath = source.absolutePath' in ui and 'sourceDurationMs' in draft
assert 'VoiceIaSync.guidance(direction, available, quickId == cue.id)' in ui
assert 'api.synthesize(key, usedModel, voiceId, requestStyle, cue.text)' in ui
assert 'promptGenerate = listOf(cue.id)' in ui and 'GeminiUsageHint("narração"' in ui
center = (base/'GeminiVoiceCenterScreen.kt').read_text()
assert 'GeminiVoiceSearch.byGender(' in center and 'GeminiVoiceSearch.byPitch(' in center
assert 'GeminiVoiceSearch.filter(' in center
assert 'val displayed = ' in center
for label in ('Femininas', 'Masculinas', 'Graves', 'Médias', 'Agudas'):
    assert label in center and label in search, label
assert 'voice?.let(GeminiVoicePresentation::label)' in ui
assert 'schemaVersion", 4' in (base/'ProjectStore.kt').read_text()
assert 'MAX_LOCAL_SPEED = 1.25' in policy
assert 'fun shorten(' in policy
assert 'requestStyle: String? = null' in draft
print('OK: sincronização assistida, cota Gemini, WAV original, filtros e compatibilidade')
