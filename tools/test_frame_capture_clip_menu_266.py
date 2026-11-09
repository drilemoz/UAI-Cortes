#!/usr/bin/env python3
"""Regressão: captura sem muxer para quadro simples e toque diferente de arrasto."""
from pathlib import Path
src=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/uaicortes'
editor=(src/'EditorScreen.kt').read_text()
menu=(src/'UnifiedEditorTimeline.kt').read_text()
snapshot=(src/'FrameSnapshotProject.kt').read_text()
renderer=(src/'FrameSnapshotRenderer.kt').read_text()
engine=(src/'ExportEngine.kt').read_text()
assert 'val directSourceFrame = !hasActivePip && !hasActiveSubtitle && !hasWatermark' in snapshot
assert 'VideoFrameSaver.save(context.applicationContext, job.sourceFrame, fileName)' in renderer
assert 'if (job.directSourceFrame)' in renderer
assert 'setMaxDelayBetweenMuxerSamplesMs' in engine
assert 'if (onRenderedFrame != null) setRemoveAudio(true)' in engine
assert 'EditedMediaItemSequence.withVideoFrom(mainItems)' in engine
assert 'detectDragGestures(onDragStart=' in menu
assert 'onTap={ onEntryTap(TimelineSelection(kind,span.id)) }' in menu
assert 'TimelineKind.MAIN_VIDEO,TimelineKind.MAIN_IMAGE->onClip(item.id)' in menu
assert '.clickable { onSelect(); onTap() }' in menu
assert 'Text("Editar",fontSize=12.sp)' not in menu
assert 'Text("Editar")' not in menu
assert 'ClipSheetAction("Editar",Icons.Default.Edit)' in editor
assert 'ClipSheetAction("Excluir",Icons.Default.Delete)' in editor
assert 'ClipSheetAction(if (active.isImage) "Duração" else "Cortar"' in editor
assert 'deletionRequested = active.id' in editor
print('OK: captura direta/composta com muxer protegido; toque menu e arrasto independente')
