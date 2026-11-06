#!/usr/bin/env python3
"""Regressão estrutural da prévia de legendas da v0.1.181.
A compilação Compose/Media3 e os gestos no aparelho precisam ser validados no CI.
"""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
s=(r/'app/src/main/java/br/com/uaicortes/SubtitleScreen.kt').read_text()
t=(r/'app/src/main/java/br/com/uaicortes/EditorTimelineUi.kt').read_text()
layout=(r/'app/src/main/java/br/com/uaicortes/SubtitleCompactLayout.kt').read_text()
assert 'Adicione uma legenda para visualizar o quadro' not in s
assert 'SubtitlePositionPreview(p, selected,' in s
assert 'initialPositionMs: Long = 0L' in s
assert 'initialPositionMs = positionMs' in (r/'app/src/main/java/br/com/uaicortes/MainActivity.kt').read_text()
assert 'val visibleCues = SubtitlePreviewPolicy.activeAt(p.subtitles, cursorMs)' in s
assert 'player.setMediaItems(p.clips.map(ClipMediaItems::playback))' in s
assert 'cursorMs = cursorMs, seekGeneration = seekGeneration' in s
assert 'onCursorChange = { cursorMs =' in s
assert 'onSeekRequest = ::requestSeek' in s
assert ('Slider(value =' in s or 'Slider(value=' in s) and 'onValueChangeFinished' in s
assert ('SubtitleListNavigation.newInterval(p.durationMs, cursorMs)' in s or 'SubtitleListNavigation.newInterval(p.durationMs,cursorMs)' in s)
assert 'requestSeek(cue.startMs)' in s
assert 'SubtitleCompactTimeline(sorted, p.clips, p.durationMs, selectedId, cursorMs,' in s
assert 'val visibleClips = remember(clips, segment, totalMs, segmentMs)' in t
assert 'SubtitleCompactLayout.clipsInWindow(' in t
assert 'TrimSegmentFrame(clip)' in t and 'SubtitleImageThumbnail(clip)' in t
assert 'detectTapGestures' in t and 'latestSeek(' in t
assert 'val currentWindow = SubtitleCompactLayout.windowFor(cursorMs, totalMs, segmentMs)' in t
assert 'fun clipsInWindow(' in layout
for name in ('SubtitleCompactLayoutTest.kt','SubtitleListNavigationTest.kt'):
    assert (r/'app/src/test/java/br/com/uaicortes'/name).exists()
assert 'mainVideoClipsAreVisibleWithoutAnySubtitle' in (r/'app/src/test/java/br/com/uaicortes/SubtitleCompactLayoutTest.kt').read_text()
print('OK: legenda sem cue, vídeos/fotos na timeline, slider, seleção e nova legenda no cursor.')
