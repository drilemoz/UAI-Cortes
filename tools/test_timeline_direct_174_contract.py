from pathlib import Path
root=Path(__file__).resolve().parents[1]
base=root/'app/src/main/java/br/com/uaicortes'
ui=(base/'UnifiedEditorTimeline.kt').read_text()
model=(base/'TimelineDirectEdit.kt').read_text()
editor=(base/'EditorScreen.kt').read_text()
project=(base/'ProjectStore.kt').read_text()
export=(base/'ExportEngine.kt').read_text()
preview=(base/'EditorTimelineUi.kt').read_text()
tests=(root/'app/src/test/java/br/com/uaicortes/TimelineDirectEditTest.kt').read_text()
assert 'detectDragGestures' in ui and 'onDragEnd=' in ui
assert 'TimelineHandle.LEFT' in ui and 'TimelineHandle.RIGHT' in ui
assert 'selected=item' in ui and 'onSelectClip(item.id)' in ui
assert 'horizontalScroll(scroll)' in ui and 'onUndo' in ui and 'onRedo' in ui
assert 'TimelineKind.AUDIO,TimelineKind.VOICE->onAudio(item.id)' in ui
assert 'onGestureStart' in ui and 'onGestureEnd' in ui
assert 'TimelineMagnet.adjust(project' in ui and 'var magnetic by remember' in ui
assert 'onTimelineEdit=' in editor and 'timelineCommit(changed)' in editor
assert 'val sourceDurations by produceState' in editor
assert 'sourceStartMs' in project and '.put("sourceStartMs",sourceStartMs.coerceAtLeast(0L))' in project
assert '.setStartPositionMs(pip.sourceStartMs)' in export
assert 'timeMs+pip.sourceStartMs' in preview
assert 'MAX_IMAGE_MS' in model and 'MIN_VIDEO_MS' in model
assert 'Mova o elemento antes de aparar' in model
assert 'fun trimmingVideoBeginningMovesContainedSubtitlesWithContent' in tests
assert 'ClipAudioBindings' not in model or 'pruneDeleted' not in model
assert 'overlapWith' in model and 'OverlayPlacement.collisions' in model
assert 'fun trimMainVideoNeverExtendsBeyondOriginalSource' in tests
assert 'fun trimPipVideoTracksSourceOffsetAndRespectsDuration' in tests
assert 'fun attachedAudioRefusesIndependentMove' in tests
print('OK: gestos por faixa, limites de origem, PiP/export, histórico e testes Kotlin')
