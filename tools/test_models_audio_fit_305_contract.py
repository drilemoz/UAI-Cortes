#!/usr/bin/env python3
"""Contratos para limites visuais, encaixe e inserção no Uai Cortes."""
from pathlib import Path
base=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/uaicortes'
read=lambda name: (base/(name+'.kt')).read_text()
ui=read('SubtitleScreen')
assert 'itemsIndexed(filtered,key=' in ui and 'pageCues' not in ui
assert 'SubtitleStudioPresets.recommended' in read('SubtitleStyleGallery')
assert 'Ver modelos antigos' not in read('SubtitleStyleGallery')
assert read('SubtitleStudioPresets').count('SubtitleStylePreset(') == 5
for control in ('"Nova legenda"', '"Legendas automáticas"', '"Personagens e filtro"', '"Definir personagens"'):
    assert control in ui
assert 'outlineWidth = 4' in read('SubtitleStudioPresets')
assert 'FONT_HEAVY' in read('SubtitleKaraokeBadge') and 'edgeInset' in read('VideoEffects')
voice=read('VoiceIaScreen')
assert 'cues.count { pendingInsertion(it, project) != null }' in voice and 'Text("Inseridas"' in voice
assert '!VoiceIaPolicy.alreadyInserted(current, cue, it)' in voice
assert 'if (VoiceIaPolicy.alreadyInserted(current, cue, take))' in voice
assert 'VoiceWindowFit.adjust(response,' in voice and 'sourceFilePath = original?.absolutePath' in voice
external=read('VoiceIaExternalAudio')
assert 'ImportedVoicePcm.read' in external and 'VoiceWindowFit.adjust(wave, target)' in external
assert 'Ajustar voz à legenda' in voice and 'track.startMs, track.durationMs' in voice
fit=read('VoiceWindowFit')
assert 'MAX_LOCAL_SPEED' in fit and 'trimSilence' in fit and 'shorten' in fit
assert 'synthesize' not in fit and 'GeminiTtsClient' not in external
assert 'BUFFER_FLAG_END_OF_STREAM' in read('ImportedVoicePcm') and 'ensureActive()' in read('ImportedVoicePcm')
print('OK: cinco modelos, navegação integral, encaixe local e inserção idempotente')
