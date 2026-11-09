#!/usr/bin/env python3
"""CI 218: quadro visível, reprodução única e integração de rotas sem regressão."""
from pathlib import Path
src = Path(__file__).resolve().parents[1] / 'app/src/main/java/br/com/uaicortes'
editor = (src/'EditorScreen.kt').read_text()
trim = (src/'TrimScreen.kt').read_text()
renderer = (src/'FrameSnapshotRenderer.kt').read_text()
saver = (src/'VideoFrameSaver.kt').read_text()
nav = (src/'EditorNavigation.kt').read_text()
assert 'FrameSnapshotProject.prepare' in editor
assert 'FrameSnapshotRenderer.save' in editor
assert 'VideoFrameSaver.saveRenderedFrame' in renderer
assert 'VisibleFrameCapture.copy(' not in editor
assert 'FrameCapturePlan.locate' in editor
assert 'MediaStore.Images.Media.IS_PENDING' in saver
assert 'TrimPreviewEndPolicy.reachedEnd' in trim
assert 'player.pause()\n                    cursorMs = clip.endMs' in trim
assert 'if (at >= clip.endMs)' not in trim
assert '"quick_split", "quick_audio", "edit_audio"' in nav
print('OK: CI218 regressão de corte/rotas; quadro agora renderizado via Media3, sem PixelCopy da tela')
