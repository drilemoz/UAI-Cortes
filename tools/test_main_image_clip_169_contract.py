"""Regressão de contrato da capa na linha principal; não substitui teste Android."""
from pathlib import Path
R = Path(__file__).resolve().parents[1]
S = R/'app/src/main/java/br/com/uaicortes'
read=lambda name:(S/name).read_text(encoding='utf-8')
assert 'val isImage: Boolean = false' in read('ProjectStore.kt')
assert '.put("isImage", isImage)' in read('ProjectStore.kt')
assert 'j.optBoolean("isImage", false)' in read('ProjectStore.kt')
assert '"schemaVersion", 4' in read('ProjectStore.kt')
assert 'MainImageInsertionUi(project' in read('MainActivity.kt')
assert 'onAddImage = mainImagePick' in read('MainActivity.kt')
# A partir da v0.1.206, a inserção de imagens ficou em Linha do tempo > Adicionar.
# Não exigir de volta o atalho duplicado da barra inferior.
editor = read('EditorScreen.kt')
timeline = read('UnifiedEditorTimeline.kt')
assert 'onAddImage=onAddImage' in editor
assert 'onAddImage:()->Unit' in timeline
assert 'Triple(Icons.Default.Image,"Imagem",' in timeline
assert 'Text("Adicionar",fontSize=11.sp)' in timeline
assert '1->onAddImage()' in timeline
assert 'Triple("Imagem", Icons.Default.AddPhotoAlternate, "image")' not in editor
assert 'MediaPlacementChoices(placement,' in read('MainImageInsertionUi.kt')
assert all(label in read('MediaInsertionUi.kt') for label in ['"Início"', '"Antes"', '"Depois"', '"Final"'])
assert 'ActivityResultContracts.OpenDocument()' in read('MainActivity.kt')
assert 'ImageClipImport.copyIntoProject' in read('MainImageInsertionUi.kt')
assert 'ImageClipImport.inspect' in read('MainImageInsertionUi.kt')
assert 'ImageClipPolicy.resizeInProject' in read('TrimScreen.kt')
assert 'ClipMediaItems.playback(clip)' in read('ExportEngine.kt')
assert '.apply { if (clip.isImage) setFrameRate(30) }' in read('ExportEngine.kt')
assert 'MediaItem.Builder().setUri(clip.uri)' in read('StaticImageClip.kt')
assert '.setImageDurationMs(clip.durationMs)' in read('StaticImageClip.kt')
assert 'if (!clip.isImage)' in read('AutoSubtitleEngine.kt')
assert 'if (clip.isImage)' in read('AudioExtractionEngine.kt')
assert 'if (clip.isImage)' in read('HomeScreens.kt')
assert (R/'app/src/test/java/br/com/uaicortes/ImageClipPolicyTest.kt').is_file()
print('OK: fotos como clipe principal, capa/posição/duração, SAF, prévia, exportação Media3, imagens sem áudio, legado v4.')
