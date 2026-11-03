#!/usr/bin/env python3
"""Contratos da tela de exportação e de privacidade (não substitui build Android)."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = root/'app/src/main/java/br/com/uaicortes'
ui = (source/'VideoSavedDialog.kt').read_text()
client = (source/'GeminiVideoCaptionClient.kt').read_text()
policy = (source/'VideoCaptionPolicy.kt').read_text()
export = (source/'ExportScreen.kt').read_text()
images = (source/'AnimateImagesScreen.kt').read_text()
assert 'videoContext: String = ""' in ui and 'showGeminiConsent' in ui
assert 'Gerar descrição com IA' in ui and 'Autorizar geração' in ui
assert 'GeminiCredentialStore(context).read()' in ui
assert 'VideoCaption(description.trim(), hashtags.trim()).combined' in ui
assert 'VideoCaptionPolicy.isValid(description, hashtags)' in ui
assert 'exportDetails.forEach' in ui and 'if (showDetails)' in ui
assert 'context.contentResolver.loadThumbnail' in ui
assert 'qualityReport=qualityReport' in export and 'videoContext=remember' in export
assert 'VideoSavedDialog(uri=uri,name=""' in images
assert 'x-goog-api-key' in client and 'responseMimeType' in client
assert 'generateContent' in client and 'GeminiKeyParser.validate(key)' in client
assert 'MAX_CHARACTERS = 100' in policy and 'words.size == 5' in policy
assert 'EXTRA_STREAM' in ui and 'EXTRA_TEXT' in ui
assert 'video/mp4' not in client and 'upload' not in client.lower()
print('OK: tela salva, texto com cota, 5 hashtags e nenhum upload de mídia para Gemini')
