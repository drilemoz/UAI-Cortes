#!/usr/bin/env python3
"""Keep header, toolbar routing and seek behavior independent from media exports."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/br/com/uaicortes"
read = lambda name: (src / f"{name}.kt").read_text()
editor, timeline, header, policy, activity = map(read, [
    "EditorScreen", "UnifiedEditorTimeline", "EditorTimelineHeader", "EditorTimelineHeaderPolicy", "MainActivity"])
assert 'EditorTimelineHeader(positionMs, project.durationMs, zoom' in timeline and 'addMenu' not in timeline
assert 'Text("Linha do tempo"' not in timeline and 'EditableSeekTime(' not in editor
assert 'ModalBottomSheet(' in header and 'onSeek(0L)' in header and 'onSeek(durationMs)' in header
assert 'EditorTimelineHeaderPolicy.position(time.text, durationMs)' in header
assert 'it in 0L..durationMs' in policy and 'seekGlobal(ms: Long)' in editor
assert 'onZoom = { zoom = it }' in timeline and 'listOf(1, 2, 4, 8)' in policy
assert 'Layout.MENU' in header and 'onMagnet' in header and 'onClick = onUndo' in header and 'onClick = onRedo' in header
assert 'Triple("Vídeo", Icons.Default.Movie, "insert_video")' in editor
assert 'Triple("Foto", Icons.Default.AddPhotoAlternate, "insert_photo")' in editor
assert '"insert_video" -> onAddVideo()' in editor and '"insert_photo" -> onAddImage()' in editor
assert 'toolbarItemWidthDp(maxWidth.value, actions.size)' in editor and 'LazyRow(state = toolsScroll' in editor
assert 'onAddImage = mainImagePick' in activity and '"append"; openGallery("editor", "append")' in activity
assert 'EditableSeekTime(' in read("PipScreen")
assert 'TimelineMagnet.adjust(project' in timeline and 'val stripWidth=maxWidth*zoom*' in timeline
print("OK: barra única, ir para, zoom/encaixe, histórico e galeria pela barra inferior")
