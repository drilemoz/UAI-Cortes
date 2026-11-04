#!/usr/bin/env python3
"""Contrato estático: consulta de modelos e geração sob consentimento; não faz requisições."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = root/'app/src/main/java/br/com/uaicortes'
client = (source/'GeminiVideoCaptionClient.kt').read_text()
model = (source/'GeminiCaptionModelPolicy.kt').read_text()
ui = (source/'VideoSavedDialog.kt').read_text()
policy = (source/'VideoCaptionPolicy.kt').read_text()
assert 'v1beta/models?pageSize=100' in client and 'nextPageToken' in client
assert 'supportedGenerationMethods' in client and 'generateContent' in model
assert 'responseJsonSchema' in client and 'responseMimeType' in client
assert 'minItems", 5' in client and 'maxItems", 5' in client
assert 'MAX_CHARACTERS = 100' in policy and 'words.size == 5' in policy
assert 'GeminiCaptionModelPolicy.validId(model)' in client
assert '"x-goog-api-key"' in client and 'readTextLimited' in client
assert 'http.errorStream' in client and 'messageForStatus' in client
assert 'generator.listModels(key)' in ui and 'selectedModel' in ui
assert 'generator.generate(key, contextToSend, selectedModel)' in ui
assert 'showGeminiConsent = true' in ui and 'Autorizar geração' in ui
assert 'GeminiCredentialStore(context).read()' in ui
assert 'putString("model",' in ui and 'MODE_PRIVATE' in ui
assert 'no MP4' not in client # nenhuma mídia é enviada ou anexada
assert '"video/mp4"' not in client and 'multipart' not in client.lower()
assert 'retry' not in client.lower() and 'Thread.sleep(' not in client
assert (root/'app/src/test/java/br/com/uaicortes/GeminiCaptionModelPolicyTest.kt').exists()
print('OK: modelos sob demanda, JSON com schema, cinco hashtags, 100 caracteres e consentimento')
