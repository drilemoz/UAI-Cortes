#!/usr/bin/env python3
"""Checagens estáticas da timeline e do movimento atômico de áudio."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
read = lambda n: (src / n).read_text(encoding='utf-8')
timeline = read('UnifiedEditorTimeline.kt')
dialog = read('QuickAudioDialog.kt')
transfer = read('MusicTrackTransfer.kt')
export = read('ExportEngine.kt')
assert 'orderedLanes(filled)' in timeline and 'it == TimelineKind.MUSIC' not in timeline
assert '"Adicionar música"' in timeline and 'TimelineEmptyLane(' in timeline and 'drawableWidth >= 40.dp' in timeline
assert 'TimelineKind.SUBTITLE->if (wasSelected) onSubtitle(item.id)' in timeline
assert 'height(lanesHeight)' in timeline and 'Estender final' in timeline
assert 'val displayTotal = total + if (extending)' in timeline and 'total+15_000L' not in timeline
assert 'val stripWidth=maxWidth*zoom*(displayTotal.toDouble()/total).toFloat()' in timeline
assert 'Mover para Música' in dialog and 'Substituir e mover' in dialog
assert 'MusicTrackTransfer.move(current, selected, inspected, existing)' in dialog
assert 'project.music != existingMusicToReplace' in transfer
assert 'music = music, audioTracks = project.audioTracks.filterNot' in transfer
assert 'sourceStartMs = current.sourceStartMs' in transfer and 'durationMs = current.durationMs' in transfer
assert 'voiceProcessors(music.volume, music.voiceTone)' in export
assert 'projectMs, playing, speed, music.loops, window.sourceStartMs' in read('EditorTimelineUi.kt')
assert '::commitWithUndo' in read('EditorScreen.kt')
print('OK: faixas compactas, régua sob demanda e música movida sem duplicar')
