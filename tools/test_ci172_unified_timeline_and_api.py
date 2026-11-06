#!/usr/bin/env python3
"""Contratos de regressão da 0.1.172. Não substituem teste Compose/Media3 em aparelho."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
src=r/'app/src/main/java/br/com/uaicortes'
editor=(src/'EditorScreen.kt').read_text()
timeline=(src/'UnifiedEditorTimeline.kt').read_text()
image=(src/'MainImageInsertionUi.kt').read_text()
activity=(src/'MainActivity.kt').read_text()
static=(src/'StaticImageClip.kt').read_text()
paths=(r/'app/src/main/res/xml/diagnostic_file_paths.xml').read_text()
record=(src/'DiagnosticRecorder.kt').read_text()
policy=(src/'ApiDiagnosticPolicy.kt').read_text()
ledger=(src/'GeminiUsageLedger.kt').read_text()
for token in ('UnifiedEditorTimeline(p,currentMs','onClip={id->','onOverlay={id->','onAudio=onSelectAudio',
              'onSubtitle=onSelectSubtitle', 'onTimelineEdit={kind,id,handle,delta,target->'):
    assert token in editor,token
for token in ('mainSpans(clips: List<Clip>, image: Boolean)', 'clip.isImage == image',
              'cursorFraction(positionMs: Long, durationMs: Long)', 'detectDragGestures',
              'detectTapGestures','TimelineSegment(span,kind,total,width,tint',
              'transitions=if(index==0)project.clips','Icons.Default.Image', 'Icons.Default.ClosedCaption',
              'Icons.Default.RecordVoiceOver','onEdit={kind,id,handle,delta,target ->'):
    assert token in timeline,token
assert 'MainImageInsertionUi(project, screen, mediaAction, selectedClip,' in activity
assert 'openGallery("editor", "image_insert")' in activity
assert '"image_insert" -> {' in activity
assert '"animate_images", "image_insert" -> GalleryScope.IMAGES_ONLY' in activity
assert 'mainImageDocumentPicker.launch(arrayOf("image/*"))' in activity
assert 'picker.launch(arrayOf("image/*"))' not in image
assert 'ImageClipImport.copyIntoProject' in image
assert 'copyIntoProject(context: Context, source: ImageClipSource)' in static
assert 'FileProvider.getUriForFile' in static and 'project_images_v1' in static
assert 'name="project_images"' in paths
for token in ('fun api(service: String', 'ApiDiagnosticPolicy.sanitize', 'lastSaveLogMs',
              'action == "project_saved"'):
    assert token in record,token
for token in ('"rate_limit"', '"remote_uncertain"', '"not_provided"',
              '"network_uncertain"', '"[A-Za-z0-9_.-]{1,48}"'):
    assert token in policy,token
assert 'DiagnosticRecorder.api("gemini"' in ledger
assert 'DiagnosticRecorder.api("gemini"' in (src/'GeminiBatchClient.kt').read_text()
assert 'DiagnosticRecorder.api("youtube"' in (src/'YouTubeSocialApi.kt').read_text()
assert 'DiagnosticRecorder.api(' in (src/'SocialExternalApis.kt').read_text()
assert 'schemaVersion", 4' in (src/'ProjectStore.kt').read_text()
print('OK: v0.1.172 timeline unificada, importação por galeria privada, diagnóstico API e compatibilidade')
