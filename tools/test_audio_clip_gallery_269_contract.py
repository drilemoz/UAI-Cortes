#!/usr/bin/env python3
"""v0.1.269: Editor de Áudio usa galeria visual e áudio por clipe é opção direta."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
audio = (src / 'AudioClipEditorScreen.kt').read_text()
menu = (src / 'EditorScreen.kt').read_text()
linked = (src / 'ClipAudioScreen.kt').read_text()
timeline = (src / 'UnifiedEditorTimeline.kt').read_text()
browser = (src / 'MediaBrowser.kt').read_text()
assert 'var videoGalleryOpen by rememberSaveable' in audio
assert 'if (videoGalleryOpen) {' in audio
assert 'DeviceMediaBrowser(' in audio
assert 'scope = GalleryScope.VIDEOS' in audio
assert 'allowMultiple = false' in audio
assert 'onSelect = { chosen ->' in audio
assert 'chosen.firstOrNull()?.takeUnless { it.isImage }' in audio
assert 'changeSource(media.uri, fromVideo = true)' in audio
assert 'Text(if (sourceIsVideo && source != null) "Trocar vídeo" else "Importar vídeo")' in audio
assert 'openDocuments = {' in audio and 'pickVideo.launch(arrayOf("video/*"))' in audio
assert 'if (videoGalleryOpen) videoGalleryOpen = false else exit()' in audio
assert 'pickAudio.launch(arrayOf("audio/*"))' in audio
assert 'GalleryScope.VIDEOS -> "Galeria de vídeos"' in browser
assert 'GalleryScope.VIDEOS -> listOf(GalleryTab.VIDEOS)' in browser
assert 'val videos = if (scope != GalleryScope.IMAGES_ONLY)' in browser
assert 'val images = if (scope != GalleryScope.VIDEOS)' in browser
assert 'val active = p.clips.firstOrNull { it.id == clipMenuId }' in menu
assert 'onClipAudio(active.id)' in menu
assert menu.index('onClipAudio(active.id)') < menu.index('if (showClipDetails && !active.isImage)')
for command in ['ClipSheetAction("Duplicar"', 'ClipSheetAction("Antes"', 'ClipSheetAction("Depois"',
                'ClipSheetAction("Excluir"', 'ClipSheetAction("Editar"', 'ClipSheetAction(if (active.isImage)']:
    assert command in menu, command
assert 'ClipAudioBindings.cloneAttached(' in menu
assert 'onClick = { picker.launch(arrayOf("audio/*")) }' in linked
assert '"Aplicar somente a este clipe"' in linked
assert '"Remover áudio deste clipe"' in linked
assert 'detectDragGestures(onDragStart=' in timeline
assert 'onTap={ onEntryTap(TimelineSelection(kind,span.id)) }' in timeline
assert '.clickable { onSelect(); onTap() }' in timeline
print('OK: galeria interna só vídeos, SAF opcional, áudio por clipe direto e arrasto preservado')
