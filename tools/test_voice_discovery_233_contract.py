#!/usr/bin/env python3
"""V0.1.233: testes de segurança e comportamento; não equivalem a build Android."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
b=r/'app/src/main/java/br/com/uaicortes'
ui=(b/'GeminiVoiceCenterScreen.kt').read_text()
pure=(b/'GeminiVoiceDiscovery.kt').read_text()
store=(b/'GeminiVoiceReviewStore.kt').read_text()
client=(b/'GeminiTtsCore.kt').read_text()
catalog=(b/'GeminiVoiceCatalog.kt').read_text()
for expected in [
    'manualReviews = storedReviews', 'recordReview(GeminiVoiceDiscovery.SIMILAR)',
    'recordReview(GeminiVoiceDiscovery.DIFFERENT)', 'Ocultar vozes que marquei como parecidas',
    'GeminiVoiceDiscovery.hiddenFromReview(', 'CompareVoiceButton(if (blindComparison && !blindRevealed) "1" else "A"',
    'CompareVoiceButton(if (blindComparison && !blindRevealed) "2" else "B"',
    'compareA = voice.id', 'compareB = voice.id',
    'Consultar todos os idiomas', 'scope.launch { fetchAllLanguages() }',
    'client.voicesAllLanguages(key)', 'GeminiVoiceDiscovery.merge(voices, fetched.voices)',
    'if (loaded && autoRefresh) fetchFromGoogle()', 'else pendingSample = voice',
    'cache.find(sampleId(voice))', 'GeminiVoiceDiscovery.isPortuguese(voice)',
    'pode consumir cota ou gerar cobrança', 'statusBarsPadding()',
]: assert expected in ui, expected
assert 'fetchAllLanguages' in catalog
assert 'fetchPages(false, fetch)' in catalog
assert 'voicesAllLanguages' in client
assert 'SIMILAR = "similar"' in pure and 'DIFFERENT = "different"' in pure
assert 'prefs.edit().putString("pairs", json.toString()).commit()' in store
assert 'apiKey' not in store and 'accessKey' not in store
assert 'MAX_VOICES = 500' in (b/'GeminiVoiceLibrary.kt').read_text()
print('OK: seleção A/B, avaliação manual, catálogo multilingue voluntário, cache e sem síntese automática')
