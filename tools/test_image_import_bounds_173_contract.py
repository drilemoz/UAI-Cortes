from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
source = (root / 'app/src/main/java/br/com/uaicortes/StaticImageClip.kt').read_text()
test = (root / 'app/src/androidTest/java/br/com/uaicortes/ImageClipImportAndroidTest.kt').read_text()
assert 'inJustDecodeBounds = true' in source
assert 'val imageStream = context.contentResolver.openInputStream(uri)' in source
assert 'imageStream.use { stream ->' in source
assert 'BitmapFactory.decodeStream(stream, null, options)' in source
assert 'require(options.outWidth > 0 && options.outHeight > 0' in source
assert not re.search(r'BitmapFactory\.decodeStream\(stream, null, options\)\s*}\s*\?:', source), 'Regressão: decodeStream retorna null com inJustDecodeBounds'
assert 'FileProvider.getUriForFile' in source
assert 'fun validJpegAndPngAreAcceptedAndCopiedToPrivateStorage' in test
assert 'fun corruptJpegIsRejectedWithInvalidImageMessage' in test
assert 'ImageClipImport.copyIntoProject(context, inspected)' in test
print('OK: importação de fotos usa dimensões, não o retorno nulo de decodeStream; testes Android preparados')
