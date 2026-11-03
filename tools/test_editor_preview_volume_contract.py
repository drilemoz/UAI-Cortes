#!/usr/bin/env python3
"""Contratos de pré-escuta. Não modifica a timeline nem valida codec real."""
from pathlib import Path
r = Path(__file__).resolve().parents[1]
b = r/'app/src/main/java/br/com/uaicortes'
editor = (b/'EditorScreen.kt').read_text()
pip = (b/'EditorTimelineUi.kt').read_text()
audio = (b/'AudioScreen.kt').read_text()
policy = (b/'EditorPreviewVolume.kt').read_text()
assert 'remember(p.id) { mutableFloatStateOf(1f) }' in editor
assert 'EditorPreviewVolume.output(clipGain, previewVolume)' in editor
assert 'EditorPreviewVolume.output(music.volume, previewVolume)' in editor
assert 'EditorPreviewVolume.output(audio.volume, previewVolume)' in editor
assert 'previewGain = previewVolume' in editor
assert 'EditorPreviewVolume.output(pip.volume, previewGain)' in pip
assert 'volume = if (pip.muted) 0f' in pip
assert 'previewVolume' not in (b/'ExportEngine.kt').read_text()
assert 'fun output(sourceVolume: Float, previewVolume: Float)' in policy
assert '"Ouvir mixagem"' in audio
assert 'TrackAudioPreview(music.uri, 0L, p.durationMs' in audio
assert 'ClipAudioBindings.resolved(p).forEach' in audio
assert 'mixPlayer.setMediaItems' in audio
assert (r/'app/src/test/java/br/com/uaicortes/EditorPreviewVolumeTest.kt').is_file()
print('OK: volume apenas na prévia, escuta da mixagem e fontes preservadas')
