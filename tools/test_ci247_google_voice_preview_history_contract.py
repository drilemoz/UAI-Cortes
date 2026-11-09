#!/usr/bin/env python3
"""v247: histórica offline e exportação voluntária, sem geração automática."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
base=r/'app/src/main/java/br/com/uaicortes'
cache=(base/'GeminiVoicePreviewCache.kt').read_text()
ui=(base/'GeminiVoiceCenterScreen.kt').read_text()
for token in ('fun listByVoice(', 'fun countsByVoice(', 'fun removeOne(', 'readIndex()', 'GeminiSavedVoicePreview(', 'filter { (_, storedVoice) -> storedVoice == voiceId }'):
 assert token in cache, token
for token in ('ActivityResultContracts.CreateDocument("audio/wav")', 'cache.find(hash)', 'ctx.contentResolver.openOutputStream(uri, "w")',
 'historyCounts', 'historyVoice', 'historyItems', 'cache.listByVoice(', 'cache.removeOne(',
 'pendingHistoryDelete', 'Nenhuma chamada Google', 'Somente prévias da frase/modelo atuais',
 'Vozes com qualquer prévia no histórico offline'):
 assert token in ui, token
assert 'scope.launch { fetchFromGoogle() }' in ui
assert 'pendingSample = voice' in ui
assert 'val requestedModel = sampleModel' in ui and 'val requestedText = sampleText()' in ui
assert 'client.synthesize(key, requestedModel, voice.id,' in ui
assert 'AlertDialog(onDismissRequest = { pendingHistoryDelete = null }' in ui
assert 'GeminiTtsClient' not in cache
assert 'GeminiVoicePreviewHistory247AndroidTest' in (r/'app/src/androidTest/java/br/com/uaicortes/GeminiVoicePreviewHistory247AndroidTest.kt').read_text()
print('OK: biblioteca de prévias offline, exportação WAV voluntária, exclusão individual e proteção da API')
