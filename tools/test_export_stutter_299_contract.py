#!/usr/bin/env python3
"""Contrato estrutural do diagnóstico de quadros. O decodificador exige Android."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/br/com/uaicortes"

def read(name):
    return (src / name).read_text(encoding="utf-8")

ui = read("ExportScreen.kt")
assert 'Text("Analisar engasgos")' in ui and "ExportStutterDialog(" in ui
assert "onAnalyze" in read("VideoSavedDialog.kt")
dialog = read("ExportStutterDialog.kt")
assert all(x in dialog for x in ("OpenDocument()", 'arrayOf("video/mp4")', "job?.cancel()", "Copiar diagnóstico"))
flow = read("ExportStutterAnalysis.kt")
assert "minOf(5_000L, clip.durationMs)" in flow and "source.clip.copy(" in flow
assert "onPreviewReady" in flow and "temporary?.delete()" in flow and "engine.cancel()" in flow
assert "Dispatchers.IO" in flow and "Dispatchers.Default" in flow and "DIFFERENT_DURATION" in flow
assert "saveToGallery" not in flow and "MediaStore" not in flow
decoder = read("ExportFrameSampling.kt")
assert "getOutputImage" in decoder and "info.presentationTimeUs" in decoder
assert "releaseOutputBuffer" in decoder and "extractor.release()" in decoder
comparison = read("ExportFrameOrder.java")
assert "previous <= next" in comparison and "seen && advances" in comparison
diag = read("DiagnosticRecorder.kt").split("internal fun exportStutter(", 1)[1].split("private fun", 1)[0]
assert "active.get()" in diag and "EXPORT_STUTTER" in diag
assert not any(x in diag for x in ("clip.uri", "clip.title", ".luma"))
assert (root / "docs/VALIDATION_0.1.299.md").is_file()
print("OK: diagnóstico opcional e local, exportação temporária, ordem visual e agregação")
