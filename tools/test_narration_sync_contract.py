#!/usr/bin/env python3
"""Contrato estático da v0.1.111. Complementar ao Kotlin/JVM; não equivale a build Android."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
base=root/'app/src/main/java/br/com/uaicortes'
ui=(base/'GeminiNarrationUi.kt').read_text()
logic=(base/'SubtitleNarrationTimeline.kt').read_text()
assert 'SubtitleNarrationTimeline.validate(segments, current.durationMs)' in ui
assert 'timedLines = SubtitleNarrationReuse.eligible(reuseSnapshot)' in ui
assert 'useTimedSubtitles = true' in ui
assert '"generate_sync"' in ui and 'confirmation = if (useTimedSubtitles' in ui
assert 'GeminiAudioFiles.save(ctx, wave)' in ui
assert 'previous == null' in ui
assert 'timedFiles = timedFiles.filterNot' in ui
assert 'SubtitleNarrationTimeline.inspect(records, current.durationMs)' in ui
assert 'Text("Revisar narração sincronizada")' in ui
assert 'Checkbox(checked = approveOverrun' in ui
assert 'inspections.any { it.blocking }' in ui
assert 'startMs = NarrationReferenceAlignment.start(result.segment, referenceStarts)' in ui
assert 'ready.size != timedLines.size' in ui
assert 'current.audioTracks + tracks' in ui
assert 'if (onSaveToAudio(changed))' in ui
assert 'sourceStartMs' not in logic
assert 'Issue.PROJECT_OVERFLOW' in logic and 'Issue.NEXT_SPEECH_OVERLAP' in logic
assert 'val playable = minOf(duration, (fresh.durationMs - position)' in ui
assert 'if (playable < 250L || playable < duration)' in ui
assert 'Gerar áudio' not in logic
print('OK: alinhamento por legenda, revisão, consentimento, salvamento atômico, sem truncar silenciosamente')
