#!/usr/bin/env python3
"""Verifica integração da retomada por lotes sem alegar build Android."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
a=(r/'app/src/main/java/br/com/uaicortes/GeminiNarrationUi.kt').read_text()
b=(r/'app/src/main/java/br/com/uaicortes/GeminiNarrationCheckpoint.kt').read_text()
model=(r/'app/src/main/java/br/com/uaicortes/ProjectStore.kt').read_text()
assert 'GeminiNarrationCheckpoint(ctx, project.id)' in a
assert 'checkpoint.recover(project)' in a and 'LaunchedEffect(checkpoint, currentDraft)' in a
assert 'currentDraft.recovered.value = true' in a
assert 'snapshotFlow { checkpoint.capture(currentDraft, latestProject.durationMs) }' in a
assert 'checkpoint.save(checkpoint.capture(currentDraft, current.durationMs))' in a
assert a.count('checkpoint.save(checkpoint.capture(currentDraft, current.durationMs))') == 2
assert 'GeminiNarrationCheckpoint.hash(wave.bytes)' in a
assert a.index('GeminiAudioFiles.save(ctx, wave)', a.index('action == "generate_sync"')) < a.index('checkpoint.save(checkpoint.capture(currentDraft, current.durationMs))',a.index('action == "generate_sync"'))
assert 'missingTimedCount' in a and '"Retomar ($missingTimedCount)"' in a
assert '"Retomar este lote com $missingTimedCount' in a
assert 'confirmation = if (useTimedSubtitles' in a
assert 'reuseSnapshot.take(SubtitleNarrationTimeline.MAX_SEGMENTS)' in a
assert 'Text("Próximas 12")' in a
assert 'GeminiAudioFiles.save(ctx, wave)' in a
assert 'gemini_narration_sessions' in b and 'context.filesDir' in b
assert 'projectId' in b and 'project.subtitles.none' in b
assert 'hash(wav) != checksum' in b and 'SHA256.matches(checksum)' in b
assert 'wav.canonicalFile.parentFile != audioDir.canonicalFile' in b
assert 'Mutex()' in b and 'serial <= committed' in b and 'out.fd.sync()' in b
assert 'return Recovery(null' in b
assert 'api.synthesize(' not in b and 'Credential' not in b
assert '.put("schemaVersion", 4)' in model
assert (r/'docs/VALIDATION_0.1.115.md').exists()
print('OK: checkpoint privado por projeto, SHA-256, retomar lotes sob aprovação, versão 0.1.115')
