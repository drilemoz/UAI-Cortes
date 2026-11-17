#!/usr/bin/env python3
"""Protege o cenário do build 241. Não substitui o teste Kotlin do app."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
path = root / 'app/src/test/java/br/com/uaicortes/VoiceIaSpeakerVoicePolicyTest.kt'
test = path.read_text(encoding='utf-8')
default, explicit = test.split('fun classicalGeneratedStyleHonorsAnExplicitSizeOf12', 1)
default = default.split('fun classicalGeneratedStyleMatchesSrtButKeepsWordTimingsAndSpeaker', 1)[1]
assert 'EditFeatures.parseSrt(' in default
assert 'SubtitleAppearanceMode.CLASSIC_SRT, SubtitleAppearancePolicy.defaultSize()' in default
assert 'assertEquals(14, imported.sizeSp)' in default
assert 'assertEquals(imported.sizeSp, converted.sizeSp)' in default
assert 'assertEquals(generated.wordTimings, converted.wordTimings)' in default
assert 'assertEquals("cliente", converted.speakerId)' in default
assert 'SubtitleAppearanceMode.CLASSIC_SRT, 12' in explicit
assert 'assertEquals(12, converted.sizeSp)' in explicit
print('OK: CI 241 — padrão 14 no estilo clássico e 12 quando escolhido')
