#!/usr/bin/env python3
"""Regras de integração ElevenLabs (não substituem build ou API real)."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
code = root / 'app/src/main/java/br/com/uaicortes'
read = lambda file: (code / file).read_text(encoding='utf-8')
center = read('SocialIntegrationsUi.kt')
main = read('MainActivity.kt')
nav = read('EditorNavigation.kt')
creds = read('ElevenLabsCredentials.kt')
api = read('ElevenLabsApi.kt')
ui = read('ElevenLabsSettingsScreen.kt')

assert 'ElevenLabs' in center and 'openElevenLabs' in center
assert '"elevenlabs_settings" -> ElevenLabsSettingsScreen(::back)' in main
assert '"elevenlabs_settings" -> "api_center"' in nav
assert 'AndroidKeyStore' in creds and 'AES/GCM/NoPadding' in creds
assert 'elevenlabs_secret_local' in creds and 'putString("sealed", sealed)' in creds
assert 'instanceFollowRedirects = false' in api and 'xi-api-key' in api
assert 'https://api.elevenlabs.io' in api
assert '/v1/user' in api and '/v2/voices?page_size=30' in api
assert '/v1/text-to-speech/' in api and 'eleven_multilingual_v2' in api
assert 'output_format=mp3_44100_128' in api
assert 'setFixedLengthStreamingMode' in api and '8 * 1024 * 1024' in api
assert 'showGenerate = true' in ui and 'if (showGenerate) AlertDialog' in ui
assert 'store.remove()' in ui and 'store.save(key)' in ui
assert 'store.read()' in ui and 'Copy' not in ui
assert 'OpenDocument()' in ui and 'CreateDocument("audio/mpeg")' in ui
assert 'elevenlabs_secret_local.xml' in (root/'app/src/main/res/xml/gemini_backup_rules.xml').read_text()
assert 'elevenlabs_secret_local.xml' in (root/'app/src/main/res/xml/gemini_data_extraction_rules.xml').read_text()
assert 'ElevenLabs272Test' in (root/'app/src/test/java/br/com/uaicortes/ElevenLabs272Test.kt').read_text()
assert '0.1.272' in (root/'CHANGELOG.md').read_text()
assert '0.1.272' in read('AppReleaseNotes.kt')
print('OK: rota ElevenLabs, chave Keystore, acesso, vozes, consentimento de consumo, backup e testes')
