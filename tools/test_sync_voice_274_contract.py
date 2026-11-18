#!/usr/bin/env python3
"""Contrato v0.1.274: sincronismo de legendas Gemini e Vozes IA sem legendas.
Checagem estrutural; não substitui compilação Android nem teste com Gemini real."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
engine = (src / 'AutoSubtitleEngine.kt').read_text(encoding='utf-8')
assert 'GAP_TOLERANCE_SAMPLES' in engine and 'writeSilence(missing)' in engine and 'emitted++' in engine
client = (src / 'GeminiSubtitleClient.kt').read_text(encoding='utf-8')
assert 'SubtitleTimingRefiner.refine(' in client and 'SubtitleVoiceEnvelope.read(checkNotNull(cachedAudio))' in client
refiner = (src / 'SubtitleTimingRefiner.kt').read_text(encoding='utf-8')
for token in ('estimateShift', 'nearestOnset', 'nearestOffset', 'MAX_GLOBAL_SHIFT_MS = 600'):
    assert token in refiner, token
assert 'text = ' not in refiner.split('fun refine')[1], 'o refinador nunca deve reescrever texto'
sync = (src / 'VoiceIaSync.kt').read_text(encoding='utf-8')
assert 'fun trimSilence' in sync and 'fun fit(' in sync and 'WSOLA' in sync
assert 'MAX_LOCAL_SPEED = 1.25' in sync
assert 'VoiceIaWavStretch.fit(' in (src / 'VoiceIaScreen.kt').read_text(encoding='utf-8')
main = (src / 'MainActivity.kt').read_text(encoding='utf-8')
assert 'VoiceIaFreeEntry(' in main
free = (src / 'VoiceIaFreeScript.kt').read_text(encoding='utf-8')
assert 'changed.copy(subtitles = project.subtitles)' in free
print('OK: v0.1.274 sincronismo de legendas e vozes sem legenda')
