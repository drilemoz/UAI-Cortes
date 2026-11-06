#!/usr/bin/env python3
"""v0.1.198: galeria unificada em ferramentas rápidas e primeiro projeto.
Validação estática de navegação/seleção. Não substitui teste Android.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
source=root/'app/src/main/java/br/com/uaicortes'
quick=(source/'QuickVideoToolsScreen.kt').read_text()
main=(source/'MainActivity.kt').read_text()
browser=(source/'MediaBrowser.kt').read_text()
assert 'var galleryOpen by rememberSaveable(mode)' in quick
assert 'OutlinedButton(onClick = { galleryOpen = true }' in quick
assert 'DeviceMediaBrowser(' in quick
assert 'scope = GalleryScope.VIDEOS' in quick and 'allowMultiple = false' in quick
assert 'chosen.firstOrNull()?.takeUnless { it.isImage }' in quick
assert 'useVideo(media.uri, persistDocumentAccess = false)' in quick
assert 'openDocuments = { picker.launch(arrayOf("video/*")) }' in quick
assert 'if (uri != null) useVideo(uri, persistDocumentAccess = true)' in quick
assert 'if (persistDocumentAccess) runCatching {' in quick
assert 'takePersistableUriPermission' in quick
assert 'BackHandler(galleryOpen || processing)' in quick
assert 'if (galleryOpen) galleryOpen = false else confirmCancel = true' in quick
assert 'pendingPicker = "new"; selectedClip = ""; openGallery("home", "new")' in main
assert '"new", "append" -> importVideoClips(chosen.map { it.uri }, persistDocumentAccess = false)' in main
assert '"new", "append" -> videoPicker.launch(arrayOf("video/*"))' in main
assert '"new", "append", "audio_extract" -> GalleryScope.VIDEOS' in main
assert 'MainImageInsertionUi(' in main
for feature in ['"Galeria de vídeos"','"Pesquisar mídias"','"Atualizar galeria"','"Procurar nos arquivos"']:
    assert feature in browser, feature
assert 'QuickVideoSplitEngine(context.applicationContext)' in quick
assert 'AudioExtractionEngine(context.applicationContext)' in quick
print('OK: v0.1.198, galeria integrada em ferramentas rápidas, projeto novo e fallback SAF')
