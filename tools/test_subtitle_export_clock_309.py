#!/usr/bin/env python3
"""Contrato estrutural do relógio; não substitui decodificar o MP4 no Android."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = root / "app/src/main/java/br/com/uaicortes"
engine = (source / "ExportEngine.kt").read_text(encoding="utf-8")
overlay = (source / "VideoEffects.kt").read_text(encoding="utf-8").split(
    "class TimedSubtitleOverlay(", 1)[1]

# Nos dois caminhos, os cues mantêm tempos absolutos e nenhum offset entra no overlay.
for layer in ("textLayers", "outputLayers"):
    assert f"{layer} += TimedSubtitleOverlay(project.subtitles)" in engine
    assert f"{layer} += TimedSubtitleOverlay(listOf(watermarkCue), singleLine = false)" in engine
assert "clipStartInProjectMs" not in overlay
assert overlay.count("val timelineMs = presentationTimeUs / 1000L") == 3
assert overlay.count("val active = cueAt(timelineMs)") == 3
assert overlay.count("SubtitleAnimations.frame(it, timelineMs)") == 3
assert "SubtitlePreviewPolicy.firstAt(cues, timelineMs)" in overlay

instrumented = (root / "app/src/androidTest/java/br/com/uaicortes/"
    "SubtitleExportClock309AndroidTest.kt").read_text(encoding="utf-8")
for term in ("verify(withPip = false)", "verify(withPip = true)",
             "sourceDurationMs = 2_000L", "startMs = 200L", "glyphPixels(retriever",
             "SubtitleWordTiming(", "AudioClipWaveform.inspect"):
    assert term in instrumented
print("OK: relógio único de exportação e regressão Android de foto, recorte, palavras e PiP")
