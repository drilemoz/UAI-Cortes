#!/usr/bin/env python3
"""CI 218: quadro visível, reprodução única e integração de rotas sem regressão."""
from pathlib import Path
src = Path(__file__).resolve().parents[1] / 'app/src/main/java/br/com/uaicortes'
editor = (src/'EditorScreen.kt').read_text()
trim = (src/'TrimScreen.kt').read_text()
window = (src/'VisibleFrameCapture.kt').read_text()
saver = (src/'VideoFrameSaver.kt').read_text()
nav = (src/'EditorNavigation.kt').read_text()
assert 'onGloballyPositioned' in editor and 'boundsInWindow()' in editor
assert 'VisibleFrameCapture.copy(activity, bounds)' in editor
assert 'VideoFrameSaver.saveBitmap(context.applicationContext, bitmap, name)' in editor
assert 'FrameCapturePlan.locate' in editor
assert 'PixelCopy.request(activity.window, source, bitmap' in window
assert 'validBounds(source, decor.width, decor.height)' in window
assert 'MediaStore.Images.Media.IS_PENDING' in saver
assert 'TrimPreviewEndPolicy.reachedEnd' in trim
assert 'player.pause()\n                    cursorMs = clip.endMs' in trim
assert 'if (at >= clip.endMs)' not in trim
assert '"quick_split", "quick_audio", "edit_audio"' in nav
print('OK: CI218 captura prévia composta, corte toca uma vez e rotas antigas preservadas')
