#!/usr/bin/env python3
"""Cut UX and bounded preview, without replacing Android runtime tests."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
read = lambda n: (src / (n + '.kt')).read_text(encoding='utf-8')
trim, timeline, policy, media = map(read, ('TrimScreen','TrimVisualTimeline','TrimMarksPolicy','StaticImageClip'))
assert 'Marcar início' in trim and 'Marcar fim' in trim and 'Prévia do corte' in trim
assert '"Aparar"' not in trim and 'Replay10' not in trim and 'Forward10' not in trim
assert 'TrimMarksPolicy.step(currentWindow, cursorMs, -1, frameFps)' in trim
assert 'TrimMarksPolicy.step(currentWindow, cursorMs, 1, frameFps)' in trim
assert 'TrimMarksPolicy.applyAll(p, marks)' in trim and 'onDone(amended)' in trim
assert 'load(keep, keep.startMs, true)' in trim and 'playbackState == Player.STATE_ENDED' in trim
assert 'ClipMediaItems.playback(clip.copy(startMs = window.startMs, endMs = window.endMs))' in trim
assert 'player.setMediaItem(media, cursorMs - window.startMs)' in trim
assert '.setStartPositionMs(clip.startMs).setEndPositionMs(clip.endMs)' in media
assert 'TrimPreviewEndPolicy.reachedEnd(localMs, window.durationMs' in trim
assert 'TrimSaveLogic.applyPending' in policy and 'TrimLogic.MIN_SEGMENT_MS' in policy
assert 'Text("Remover trecho")' in trim and 'val removal = removing' in trim
assert 'TrimVisualMath.timeInClip(' in timeline and 'cursorMs - clipStartMs' in timeline
assert 'handleStartMs' in timeline and 'handlesEnabled' in timeline
assert 'TrimTimelineEdit.split(it, clip.id, point)' in trim
assert (root/'app/src/test/java/br/com/uaicortes/TrimMarks292Test.kt').is_file()
print('OK: marcações, alças, quadro aproximado, prévia limitada e edição sem alterar a mídia')
