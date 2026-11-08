#!/usr/bin/env python3
"""Contrato v0.1.234: comparação sem nomes, exclusivamente offline, sem romper seletores existentes."""
from pathlib import Path
b=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/uaicortes'
ui=(b/'GeminiVoiceCenterScreen.kt').read_text()
pure=(b/'GeminiVoiceDiscovery.kt').read_text()
for item in [
 'GeminiVoiceDiscovery.nextOfflinePair(', 'offlineCandidates.shuffled()',
 'blindComparison = true', 'blindRevealed = false',
 '"Voz oculta"', '"Revelar nomes sem avaliar"', '"Teste cego"',
 'if (blindComparison && value != null) blindRevealed = true',
 'else if (blindLabel != null)', 'status = if (blindLabel != null)',
 'if (compareB == voice.id) compareB = null',
 'if (compareA == voice.id) compareA = null',
 'compareOpen && !(blindComparison && !blindRevealed)',
 'recordReview(GeminiVoiceDiscovery.SIMILAR)',
 'recordReview(GeminiVoiceDiscovery.DIFFERENT)',
 'if (loaded && autoRefresh) fetchFromGoogle()',
 'else pendingSample = voice',
]:
    assert item in ui, item
assert 'distinctPairs' in pure and 'if (!conflicts)' in pure
assert 'GeminiVoiceLibrary.MAX_VOICES' in pure
assert 'client.synthesize(' in ui and 'pendingSample = voice' in ui
assert 'apiKey' not in (b/'GeminiVoiceReviewStore.kt').read_text()
print('OK: comparação cega usa prévias locais, revela após avaliação; nenhuma geração automática; sem ocultação contraditória')
