#!/usr/bin/env python3
"""Contratos de integração da 286. Os testes JVM cobrem o áudio e a política de cortes."""
from pathlib import Path
root = Path(__file__).resolve().parents[1] / 'app/src/main/java/br/com/uaicortes'
read = lambda name: (root / (name + '.kt')).read_text(encoding='utf-8')
screen = read('StandaloneTextToVoiceScreen')
controls = read('VoiceStudioControls')
core = read('GeminiTtsCore')
assert 'mutableStateOf(VoiceStudioPlan.AUTO)' in screen and 'VoiceStudioPlan.suggest(text)' in screen
assert 'val requested = textModel' in screen and 'val requestedModel = model' in screen
assert 'Gemini · sem API' in controls and 'enabled = false' in controls
assert 'text = proposal; proposedText = null' in screen and 'Manter original' in screen
assert 'plan.indices.filter { current[it] == null }' in screen
assert 'batchDraft.save(request)' in screen and 'batchDraft.load()' in screen
assert 'batch.save(i, wave)' in screen and 'NonCancellable + Dispatchers.IO' in screen
assert 'Montar WAV' in controls and 'if (complete == scenes.size) !overflow' in controls
assert 'VoiceStudioPlan.measured(excess)' in controls and 'Configur' in controls
assert 'Quot' in core and 'violation.optString("quotaId")' in core
assert '.startsAtZero == true) 0L else cursor' in read('StandaloneVoiceTimelineImport')
print('OK: estúdio automático, escolha isolada, pendências, WAV e cota estruturada')
