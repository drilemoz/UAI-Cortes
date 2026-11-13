#!/usr/bin/env python3
"""Revisão estática dos dois riscos identificados na v0.1.270."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
b=r/'app/src/main/java/br/com/uaicortes'
a=(b/'ExportAudioPolicy.kt').read_text()
i=(b/'ExportMp4Inspector.kt').read_text()
e=(b/'ExportEngine.kt').read_text()
s=(b/'ExportSubtitlePolicy.kt').read_text()
t=(r/'app/src/androidTest/java/br/com/uaicortes/ExportPipelineSmokeAndroidTest.kt').read_text()
assert 'project.overlays.any' not in a.split('fun hasRequiredAudio')[1]
assert 'fun sourceHasAudio(context: Context, uri: Uri): Boolean' in i
assert 'ExportMp4Inspector.sourceHasAudio(context, Uri.parse(pip.uri))' in e
assert 'pipAudioAvailability[pip.uri] != true' in e
assert 'val expectedAudio = ExportAudioPolicy.hasRequiredAudio(project) ||' in e
assert 'it.endMs > 0L && it.startMs < durationMs' in s
assert 'cue.startMs.coerceAtLeast(0L)' in s
assert 'pipAudioProbeDifferentiatesVideoOnlyFromVideoWithAudio' in t
assert (r/'app/src/androidTest/assets/export_smoke_video_only.mp4').is_file()
from check_version_sync import check
version, code = check(r)
assert int(version.rsplit('.', 1)[-1]) >= 271 and code >= 272
print('OK: revisão 0.1.271 — PiP sem áudio, cue negativa visível, versionamento')
