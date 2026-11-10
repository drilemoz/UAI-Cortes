#!/usr/bin/env python3
"""Contratos de v0.1.270; análise estática não substitui codec Android real."""
from pathlib import Path
p=Path(__file__).resolve().parents[1]
b=p/'app/src/main/java/br/com/uaicortes'
e=(b/'ExportEngine.kt').read_text()
a=(b/'ExportAudioPolicy.kt').read_text()
s=(b/'ExportSubtitlePolicy.kt').read_text()
r=(b/'ExportMp4Inspector.kt').read_text()
ui=(b/'ExportScreen.kt').read_text()
report=(b/'ExportQualityReport.kt').read_text()
assert 'ExportAudioPolicy.activeExtraTracks(project)' in e
assert 'verifiedMediaDuration(context, Uri.parse(audio.uri))' in e
assert 'mediaDuration(context, Uri.parse(audio.uri))' not in e
assert 'project.music?.takeIf { ExportAudioPolicy.audible(it.volume) }' in e
assert 'require(sourceDuration > 0L)' in e
assert 'ExportAudioPolicy.window(' in e and '.setEndPositionMs(window.sourceStartMs + window.playableMs)' in e
assert 'ExportMp4Inspector.requirePlayable(' in e
assert 'ExportMp4Inspector.sourceHasAudio(context, Uri.parse(pip.uri))' in e
assert 'pipAudioAvailability[pip.uri] != true' in e
assert 'ExportMp4Inspector.inspectSaved(' in e
assert 'context.contentResolver.delete(uri, null, null)' in e
assert 'fun audible(volume: Float)' in a and 'fun window(' in a
assert 'java.util.PriorityQueue<Long>' in s
assert 'fun inspectSaved(' in r and 'streams.durationMs > 0L' in r
assert 'Áudio e legendas na exportação' in ui and 'NÃO a mixagem final' in ui
assert 'subtitleSummary.warnings' in ui
assert 'Faixa de áudio no MP4 salvo:' in report
for f in ('ExportAudioPolicyTest.kt', 'ExportSubtitlePolicyTest.kt'):
    assert (p/'app/src/test/java/br/com/uaicortes'/f).exists(), f
assert 'inspectorRecognizesRealVideoAndAudioFromFixture' in (p/'app/src/androidTest/java/br/com/uaicortes/ExportPipelineSmokeAndroidTest.kt').read_text()
print('OK: v270 audio legivel, sem 1s ficticio, MP4 confirmado, legendas diagnosticadas')
