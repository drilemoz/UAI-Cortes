#!/usr/bin/env python3
"""Contrato v0.1.231: UI compacta, A/B, cache por texto e filtros sem inferência."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
base = root/'app/src/main/java/br/com/uaicortes'
ui=(base/'GeminiVoiceCenterScreen.kt').read_text()
search=(base/'GeminiVoiceSearch.kt').read_text()
identity=(base/'GeminiPreviewIdentity.kt').read_text()
text=(base/'GeminiVoiceSampleText.kt').read_text()
aliases=(base/'GeminiVoiceAliases.kt').read_text()
for item in ('statusBarsPadding()', 'GeminiVoiceAliases.title', 'GeminiVoiceSearch.byMetadata(',
             'MetadataFilter("Sotaque"', 'MetadataFilter("Finalidade"',
             'MetadataFilter("Personalidade"', 'compareA', 'compareB', 'Escolher A', 'Escolher B',
             'Gerar prévia', 'pode consumir cota ou gerar cobrança',
             'GeminiPreviewIdentity.idForText', 'visibleCount += 30'):
    assert item in ui, item
assert 'voice.id' in ui and 'cache.find(sampleId(voice))' in ui
assert 'filterWithAliases' in search and 'fieldValue' in search
assert 'idForText' in identity and 'model, voiceId, styleInstruction, TEXT' in identity
assert 'SHORT_TEXT' in text and 'CUSTOM' in text
assert 'GeminiVoiceFavorites.valid(id)' in aliases
assert 'GeminiVoiceAliases.title' in (base/'StandaloneTextToVoiceScreen.kt').read_text()
assert (root/'app/src/test/java/br/com/uaicortes/GeminiVoiceEnhancementsTest.kt').is_file()
print('OK: central 0.1.231 com filtros por API, apelidos, prévias e comparação A/B')
