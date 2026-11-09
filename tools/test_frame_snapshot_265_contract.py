#!/usr/bin/env python3
"""Quadro em resolução real: evita regressão para screenshot com bordas."""
from pathlib import Path
src=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/uaicortes'
editor=(src/'EditorScreen.kt').read_text()
engine=(src/'ExportEngine.kt').read_text()
snapshot=(src/'FrameSnapshotProject.kt').read_text()
renderer=(src/'FrameSnapshotRenderer.kt').read_text()
saver=(src/'VideoFrameSaver.kt').read_text()
assert 'FrameSnapshotProject.prepare' in editor
assert 'FrameSnapshotRenderer.save' in editor
assert 'VisibleFrameCapture.copy' not in editor
assert 'onRenderedFrame' in engine and 'onRenderedFrame(target)' in engine
assert 'return saveBitmap(context, bitmap, name)' in saver
assert 'MediaMetadataRetriever.OPTION_CLOSEST' in saver
assert 'sourceShortSide(context' in editor
assert 'project.copy(clips = listOf(' in snapshot
assert 'subtitle' in snapshot.lower() and 'overlays' in snapshot
assert 'transitions = emptyList()' in snapshot
assert 'FrameSnapshotTiming.overlap' in snapshot
assert 'onRenderedFrame = { file ->' in renderer
assert 'saveRenderedFrame' in renderer
assert 'target.delete()' in engine
assert 'MediaStore.Images.Media.IS_PENDING' in saver
print('OK: quadro via Media3, PiP e legendas, qualidade por resolução real; sem bordas de UI')
