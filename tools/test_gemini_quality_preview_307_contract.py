#!/usr/bin/env python3
from pathlib import Path
base=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/uaicortes'
read=lambda name: (base/(name+'.kt')).read_text()
client=read('GeminiSubtitleClient')
request=read('GeminiTranscriptionRequest')
assert '.put("type", "file")' in request and '.put("file_uri", uri)' in request
assert 'val state = response.optString("state").ifBlank' in client
assert 'DiagnosticRecorder.subtitleCloudFailure("transcribe", issue)' in client
fault=read('SubtitleCloudFailure')
assert 'http=$it' in fault and 'needsRecoveryReview' in fault
auto=read('AutoSubtitleScreen')
assert 'issue?.needsRecoveryReview == true' in auto and 'else geminiCheckpoint.clear()' in auto
assert 'Ajuda sobre transcrição Gemini' in auto and 'Nenhuma legenda alterada.' in auto
assert 'Corrigir português · Gemini' in read('SubtitleScreen')
assert 'Aplicar selecionadas' in read('SubtitleProofreadDialog')
assert 'after ?: cue' in read('SubtitleProofreadPolicy')
preview=read('TransitionVideoPreview')
for check in ('JunctionPreviewProject.compactDirectCut(', 'override fun onRenderedFirstFrame()',
              'withTimeoutOrNull(45_000L)', 'withTimeoutOrNull(9_000L)', 'renderRequested = false'):
    assert check in preview, check
assert 'Priorizar nitidez · 1080p / 28 Mb/s' in read('ExportScreen')
assert 'bitratePresetName=VideoExportFormatPolicy.recommendedBitrate' not in read('ExportScreen')
assert 'requested_kbps=' in read('DiagnosticRecorder') and 'measured_kbps=' in read('DiagnosticRecorder')
print('OK: transcrição REST, revisão de texto, diagnósticos de qualidade e primeiro quadro')
