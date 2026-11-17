#!/usr/bin/env python3
"""Contrato estrutural. Gestos e falhas reais de decodificação exigem Android."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/br/com/uaicortes"
def read(name):
    return (src / (name + ".kt")).read_text(encoding="utf-8")

timeline, editor = read("UnifiedEditorTimeline"), read("EditorScreen")
ruler = timeline.split("internal fun TimelinePlayheadRuler", 1)[1].split("private fun TimelineSegment", 1)[0]
assert "private val rulerHeight = 48.dp" in timeline
assert "detectTapGestures" in ruler and "detectHorizontalDragGestures" in ruler
assert "activeStart(); update(it.x)" in ruler and "onDragCancel = ::finish" in ruler
assert "finally { finish() }" in ruler and "setProgress" in ruler
assert "if (dragMs != null)" in ruler and "formatPipMs(shownMs)" in ruler
assert "onGestureStart={scrubbing=true;player.pause()}" in editor
assert "val maxLaneViewport" in timeline and "horizontalScroll(scroll)" in timeline
preview, engine = read("TransitionVideoPreview"), read("ExportEngine")
assert "TransitionDiagnosticContext(boundary, kind)" in preview
assert all(x in preview for x in ("PLAY_FAILED", "RENDER_FAILED", "RENDER_READY", "CANCELLED"))
assert "TransitionDiagnosticContext(join.boundary, join.kind)" in engine
assert "TransitionStage.FRAME_PREP_FAILED" in engine and "TransitionStage.PLAN_FAILED" in engine
assert "decodePipPreviewImage(context.contentResolver" in read("EditorTimelineUi")
assert all(x in read("PipPreviewImage") for x in ("PREVIEW_OPEN", "PREVIEW_DECODE", "INVALID_IMAGE"))
assert "10_000L" in read("DiagnosticRecorder")
assert "Target.MAIN_IMAGE" in read("MainImageInsertionUi") and "Target.pip(isImage)" in read("MediaImportTasks")
assert '-> "unknown"' in read("EditorDiagnosticPolicy") or 'else "unknown"' in read("EditorDiagnosticPolicy")
assert (root / "docs/VALIDATION_0.1.298.md").is_file()
print("OK: régua, equilíbrio do arrasto, estágios e privacidade")
