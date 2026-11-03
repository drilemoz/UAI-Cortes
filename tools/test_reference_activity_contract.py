#!/usr/bin/env python3
"""Check local-only audio energy analysis and explicit user consent boundaries."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
base = root / 'app/src/main/java/br/com/uaicortes'
probe = (base / 'ReferenceAudioProbe.kt').read_text()
activity = (base / 'ReferenceAudioActivity.kt').read_text()
ui = (base / 'AudioReferenceAlignmentUi.kt').read_text()
assert 'MediaExtractor()' in probe and 'MediaCodec.createDecoderByType' in probe
assert 'Dispatchers.IO' in probe and 'currentCoroutineContext().ensureActive()' in probe
assert 'SystemClock.elapsedRealtime()' in probe
assert 'extractor.release()' in probe and 'decoder?.release()' in probe
assert 'WINDOW_BEFORE_MS' in activity and 'MIN_REGION_MS' in activity
assert 'fun detect(' in activity and 'fun window(' in activity
assert 'ReferenceAudioProbe.analyze(context, slices' in ui
assert 'ReferenceActivityWaveform(activity' in ui
assert '"Usar sugestão neste trecho"' in ui
assert '"Aplicar horários"' in ui and '"Cancelar"' in ui
assert 'marks + (selectedSegment.cueId to suggestion)' in ui
assert 'onApply(NarrationReferenceAlignment.cleaned(segments, marks))' in ui
assert 'synthesize' not in probe.lower() and 'upload' not in probe.lower()
assert (root/'app/src/test/java/br/com/uaicortes/ReferenceAudioActivityTest.kt').exists()
assert (root/'docs/VALIDATION_0.1.113.md').exists()
print('OK: energy waveform local, scoped decoder, suggestions only with explicit confirmation')
