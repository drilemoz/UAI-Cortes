#!/usr/bin/env python3
"""Nomes/MIME, temporização compartilhada, Downloads e recuperação explícita de 503."""
from pathlib import Path

base = Path(__file__).resolve().parents[1] / 'app/src/main/java/br/com/uaicortes'
read = lambda name: (base / (name + '.kt')).read_text()
export = read('SubtitleFileExport')
writer = read('SubtitleDocumentWriter')
screen = read('SubtitleScreen')
proof = read('SubtitleProofreadDialog')
assert 'SRT("srt", "application/x-subrip")' in export
assert 'TXT("txt", "text/plain")' in export
assert 'append(time(cue.startMs)).append(" --> ").append(time(cue.endMs))' in export
assert 'fun render(cues: List<SubtitleCue>)' in export
assert 'val text = SubtitleFileExport.render(cues)' in writer
assert 'ActivityResultContracts.CreateDocument(SubtitleFileExport.Format.SRT.mimeType)' in screen
assert 'ActivityResultContracts.CreateDocument(SubtitleFileExport.Format.TXT.mimeType)' in screen
assert 'Importar SRT ou TXT com horários' in screen and 'EditFeatures.parseSrt(content)' in screen
assert 'MediaStore.Downloads.EXTERNAL_CONTENT_URI' in writer and 'IS_PENDING' in writer
assert 'if (!published) runCatching { resolver.delete(uri, null, null) }' in writer
assert 'hasExpectedExtension(actual)' in writer and 'hasExpectedExtension(actual)' in screen
assert 'failureHttp == 503' in proof and 'onClick = { request(model.alternative) }' in proof
assert 'DiagnosticRecorder.subtitleCloudFailure("proofread", info)' in proof
assert 'using.id, "subtitle_proofread"' in proof
assert 'ClipData.newPlainText("Legendas com horários", SubtitleFileExport.render(cues))' in proof
assert 'gemini-3.5-flash-lite' in read('SubtitleProofreadPolicy')
print('OK: SRT, TXT temporizado e alternativa explícita para 503')
