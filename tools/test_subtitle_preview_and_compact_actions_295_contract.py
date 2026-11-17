#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
def files(name):
    return (src / f'{name}.kt').read_text()

clock = files('SubtitleLiveClock')
assert 'while (true) withFrameNanos' in clock and 'player.currentPosition' in clock
assert 'if (playing) clock.value else fallbackMs' in clock
assert all('rememberSubtitleLiveClock(' in files(x) for x in ('EditorScreen', 'SubtitleScreen'))
word = files('SubtitleAnimations')
assert 'SINGLE_WORD ->' in word and 'Frame(index?.let' in word and 'WORD_REVEAL ->' in word
assert 'SubtitleWordEffects.singleWordIndex(cue, absoluteMs)' in word
assert 'hasIndividualTimes(cue)' in files('SubtitleWordEffects')
sample = files('JunctionPreviewProject')
assert 'TransitionBatch.apply(project, index' in sample
assert 'copy(' not in sample
assert 'setStartPositionMs(job.playbackStartMs).setEndPositionMs(job.playbackEndMs)' in files('TransitionVideoPreview')
assert all('frameMode = outputMode.frameMode' in files(x) for x in ('TransitionVideoPreview', 'SubtitleExportPreview'))
assert 'CompactActionTile("Rascunho"' in files('ProjectDraftUi')
assert 'CompactActionTile("Apagar", Icons.Default.DeleteOutline, danger = true)' in files('DiagnosticScreen')
assert 'CompactActionTile("Reiniciar"' in files('GitHubUpdateUi')
assert 'AvailableUpdateNotes(release)' in files('GitHubUpdateUi')
assert 'release.previousNotes.forEach' in files('GitHubUpdateUi')
assert 'parts[1] == projectId' not in files('ProjectMediaCopies')
assert 'Mídias protegidas. Salve' not in files('ProjectMediaProtectionLifecycle')
assert 'cues.count(SubtitleWordEffects::hasIndividualTimes)' in files('DiagnosticRecorder')
assert 'cursor_delta_ms' in files('DiagnosticRecorder') and 'operation=protection' in files('DiagnosticRecorder')
assert (root / 'app/src/androidTest/java/br/com/uaicortes/SubtitlePreviewSync295AndroidTest.kt').is_file()
assert (root / 'docs/VALIDATION_0.1.295.md').is_file()
print('OK: relógio do player, palavra isolada, mixagem da junção e ações compactas')
