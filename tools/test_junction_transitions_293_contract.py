#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
app = root / 'app/src/main/java/br/com/uaicortes'
plan = (app / 'JunctionTransitionPlan.kt').read_text()
render = (app / 'JunctionTransitionRender.kt').read_text()
engine = (app / 'ExportEngine.kt').read_text()
preview = (app / 'TransitionVideoPreview.kt').read_text()
sample = (app / 'JunctionPreviewProject.kt').read_text()
sheet = (app / 'TransitionScreen.kt').read_text()
model = (app / 'ProjectStore.kt').read_text()
editor = (app / 'EditorScreen.kt').read_text()

assert all(kind in plan for kind in ['DISSOLVE', 'PUSH_LEFT', 'PUSH_RIGHT'])
assert 'buildList<Join> {' in plan and 'if (transition.kind !in kinds) return@forEach' in plan
assert 'contains(transition.kind)' not in plan
assert 'aMs / 2L, bMs / 2L' in plan and 'a.sourceDurationMs - a.endMs < halfMs' in plan
assert 'b.startMs < halfMs' in plan and 'mainAlpha = if (before) 1f - p else p' in plan
assert 'setRemoveAudio(true)' in render and 'setOverlayFrameAnchor(-x, 0f)' in render
assert 'setBackgroundFrameAnchor(x, 0f)' in render
assert 'JunctionTransitionFrames.prepare(' in engine and 'withContext(Dispatchers.IO)' in engine
assert 'pipsTopFirst.isNotEmpty() || joins.isNotEmpty()' in engine
assert 'setVideoCompositorSettings(JunctionVideoCompositor(compositorPips, joins))' in engine
assert 'outputLayers += TimedSubtitleOverlay(project.subtitles)' in engine
assert engine.index('sequences += mainSequence') < engine.index('sequences += junctionVideoTrack(')
assert engine.count('clearTransitionFrames()') >= 4
assert 'val durationMs get() = clips.sumOf { it.durationMs }' in model
assert 'JunctionPreviewProject.prepare(' in preview
assert 'engine.start(job.project, 360' in preview and 'onPreviewReady = { file = it }' in preview
assert 'REPEAT_MODE_ALL' in preview and 'file?.delete()' in preview
assert 'minOf(a.durationMs, 1100L)' in sample and 'TransitionBatch.apply(project, index' in sample
assert 'setStartPositionMs(job.playbackStartMs).setEndPositionMs(job.playbackEndMs)' in preview
assert 'TransitionFramePreview' not in sheet and 'ModalBottomSheet(' in sheet and 'Aplicar nesta junção' in sheet
assert 'suspendPreviewForPanel' in editor and 'player.clearMediaItems()' in editor
assert (root / 'app/src/test/java/br/com/uaicortes/JunctionTransition293Test.kt').is_file()
assert (root / 'app/src/androidTest/java/br/com/uaicortes/JunctionTransition293AndroidTest.kt').is_file()
for color in ('red', 'blue'):
    assert (root / f'app/src/androidTest/assets/junction_293_{color}.mp4').is_file()
print('OK: sobreposição na junção, relógio preservado, vídeo de prévia e descarte de temporários')
