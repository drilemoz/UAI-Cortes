#!/usr/bin/env python3
"""Contrato estrutural do caminho visual. Pixels do MP4 exigem o Android."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/br/com/uaicortes"
engine = (src / "ExportEngine.kt").read_text()
photo = (src / "TimedImagePipOverlay.kt").read_text()
assert "topFirst.takeWhile { it.isImage }" in photo
assert "extends BitmapOverlay" in photo or ": BitmapOverlay()" in photo
assert engine.count("for (pip in compositorPips)") == 1
assert "pipsTopFirst.drop(timedPhotosTopFirst.size)" in engine
assert "JunctionVideoCompositor(compositorPips, joins)" in engine
assert engine.index("outputLayers += timedPhotosTopFirst.asReversed()") < engine.index(
    "outputLayers += TimedSubtitleOverlay(project.subtitles, 0L)")
assert "else transparent" in photo and "else hidden" in photo and "setAlphaScale(0f)" in photo
assert "timeUs >= startUs && timeUs - startUs < pip.durationMs * 1_000L" in photo
assert "OverlayEditorLogic.dimensions" in photo and "OverlayEditorLogic.clampCenter" in photo
assert "TimedImagePipOverlay.read(context, uri, maxEdge)" in engine
assert (root / "app/src/androidTest/java/br/com/uaicortes/ImagePipFlashAndroidTest.kt").is_file()
print("OK: fotos reaproveitam o quadro principal, z-order preservado e horários absolutos")
