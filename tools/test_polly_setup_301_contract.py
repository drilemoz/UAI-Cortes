#!/usr/bin/env python3
"""Contratos da tela. Teste de conexão e reprodução real exigem conta AWS e Android."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/br/com/uaicortes"
ui = (src / "PollySettingsScreen.kt").read_text()
policy = (src / "PollySetupPolicy.kt").read_text()
assert all(x in ui for x in (
    '"ID da chave de acesso"', '"Chave de acesso secreta"', '"Token de sessão"',
    'Text("Testar conexão")', 'Text("Testar voz")', 'Text("Salvar credenciais")',
    'PollyExpandable("Gerenciar credenciais"', 'PollyExpandable("Consumo"'))
assert 'Text("Access Key ID")' not in ui and 'Text("Secret Access Key")' not in ui
assert 'val canTest = source != PollySetupPolicy.Source.NONE && busy == null' in ui
assert 'editing && id.isNotBlank() && secret.isNotBlank() -> Source.FORM' in policy
assert 'PollySetupPolicy.Source.FORM -> draft()' in ui
assert 'PollySetupPolicy.Source.SAVED -> withContext(Dispatchers.IO)' in ui
connection = ui.split('fun refreshVoices()', 1)[1].split('fun generate()', 1)[0]
assert 'PollyApi.voices(credentialsForTest(which))' in connection and 'synthesize' not in connection
voice = ui.split('fun generate()', 1)[1].split('    Column(Modifier.fillMaxSize()', 1)[0]
assert voice.count('PollyApi.synthesize') == 1 and 'usage.allows(sample)' in voice
assert 'play(pending)' in voice and 'pending.delete()' in voice
assert 'dialog = "generate"' in ui and '"generate" -> generate()' in ui
assert 'EXTRA_IS_SENSITIVE' in ui and 'store.save(newCredentials)' in ui
print("OK: tela do Polly em português, testes antes de salvar, confirmação e consumo")
