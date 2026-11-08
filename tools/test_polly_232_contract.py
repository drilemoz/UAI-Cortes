#!/usr/bin/env python3
"""Valida a rota, segurança, integração e versionamento Polly sem simular chamadas AWS."""
from pathlib import Path
import re
ROOT = Path(__file__).resolve().parents[1]
APP = ROOT/'app/src/main/java/br/com/uaicortes'
read = lambda p: (APP/p).read_text(encoding='utf-8')
api = read('PollyApi.kt')
creds = read('PollyCredentials.kt')
ui = read('PollySettingsScreen.kt')
ledger = read('PollyUsage.kt')
main = read('MainActivity.kt')
center = read('SocialIntegrationsUi.kt')
assert 'polly_settings' in main and 'PollySettingsScreen(::back)' in main
assert 'Amazon Polly' in center and 'openPolly' in center
assert 'AndroidKeyStore' in creds and 'AES/GCM/NoPadding' in creds
assert 'prefs.edit().putString("sealed", blob)' in creds
assert 'SigV4' in api and 'polly.${c.region}.amazonaws.com' in api
assert 'instanceFollowRedirects = false' in api
assert '/v1/voices' in api and '/v1/speech' in api
assert 'x-amzn-RequestCharacters' in api
assert 'return@withContext voices.values' in api
assert '1000' in ui and 'dialog = "generate"' in ui
assert 'PollyApi.synthesize' in ui and 'usage.allows(sample)' in ui
assert 'CreateDocument("application/json")' in ui and 'OpenDocument()' in ui
assert 'polly_usage_local' in ledger and 'GeminiUsage' not in ledger
assert 'secretAccessKey' not in ledger and 'Text", text' not in ledger
assert 'versionCode = 233' in (ROOT/'app/build.gradle.kts').read_text()
assert '0.1.232' in (ROOT/'VERSION').read_text()
assert 'Polly232Test' in (ROOT/'app/src/test/java/br/com/uaicortes/Polly232Test.kt').read_text()
assert not re.search(r'(?m)^\s*val\s+\w+\s*=\s*"AKIA[A-Z0-9]{16}"', creds)
print('OK: Polly 0.1.232, rota, SigV4, confidencialidade, consumo e testes presentes')
