#!/usr/bin/env python3
"""Contratos estruturais; NÃO substituem testes visuais/codificador Android."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
b=r/'app/src/main/java/br/com/uaicortes'
e=(b/'ExportEngine.kt').read_text()
ui=(b/'EditorScreen.kt').read_text()
fx=(b/'VideoEffects.kt').read_text()
motion=(b/'ClipMotionTransitionEffect.kt').read_text()
order=(b/'ExportLayerOrder.kt').read_text()
smoke=(r/'app/src/androidTest/java/br/com/uaicortes/ExportPipelineSmokeAndroidTest.kt').read_text()
assert 'val pipsTopFirst = ExportLayerOrder.media3TopFirst(' in e
assert 'for (pip in pipsTopFirst)' in e
assert 'PipCompositor(pipsTopFirst)' in e
assert 'setEffects(Effects(emptyList(),' in e
assert 'TimedSubtitleOverlay(project.subtitles, 0L)' in e
assert 'if (!postCompositionText)' in e
assert 'fun <T> media3TopFirst(' in order and '.asReversed()' in order
assert 'SubtitlePreviewPolicy.firstAt(p.subtitles, currentMs)' in ui
assert 'SubtitlePreviewPolicy.firstAt(cues, t)' in fx
assert 'TransitionEffects.motionAt(local, clip.durationMs' in ui
assert 'TransitionEffects.motionAt(presentationTimeUs / 1000L' in motion
assert 'clipToBounds()' in ui
assert 'exportPipWithSubtitleAboveCompositorProducesReadableMp4' in smoke
assert 'context.contentResolver.delete(' in smoke
assert (r/'app/src/test/java/br/com/uaicortes/ExportLayerOrderTest.kt').is_file()
assert (r/'docs/VALIDATION_0.1.119.md').is_file()
print('OK: ordem do PiP, texto pós-composição, cue válido, zoom/pan e smoke Android previstos')
