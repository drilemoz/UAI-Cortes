#!/usr/bin/env python3
"""Verificação estática do banco seguro de chaves e falha controlada v0.1.130.
Não substitui Android Keystore, Gradle ou testes reais na API.
"""
from pathlib import Path
import re
p = Path(__file__).resolve().parents[1]
base = p/'app/src/main/java/br/com/uaicortes'
store=(base/'GeminiCredentialStore.kt').read_text()
fail=(base/'GeminiKeyFailover.kt').read_text()
policy=(base/'GeminiFailoverPolicy.kt').read_text()
ui=(base/'GeminiKeyPoolSettings.kt').read_text()
settings=(base/'GeminiSettingsUi.kt').read_text()
assert '"ciphertext"' in store and '"profiles_v2"' in store  # legado preservado
assert 'AES/GCM/NoPadding' in store and 'AndroidKeyStore' in store
assert 'profiles().size < 8' in store and 'profiles().none' in store
assert 'fun add(' in store and 'fun rename(' in store and 'fun removeProfile(' in store
assert 'setAutoFallback' in store and 'getBoolean("auto_fallback_v2", false)' in store
assert '10L * 60 * 1000' in store and 'pause_' in store
assert 'statusCode == 429' in fail and 'error.http == 429' in fail
assert 'GeminiFailoverPolicy.canSwitch' in fail
assert 'candidate.projectId in triedProjects' in fail
assert 'temporarilyBlocked' in fail and 'markExhausted' in fail
assert 'store.select(candidate.id)' in fail and 'markAutomaticSwitch' in fail
assert 'originProject != nextProject' in policy and 'code == 429' in policy
assert 'onCheckedChange' in ui and 'confirmAuto' in ui and 'Text("Ativar")' in ui
assert 'Nenhuma tentativa ocorre em segundo plano.' in ui
assert 'GeminiKeyPoolSettings(store)' in settings
for filename, client in [('GeminiTtsCore.kt','GeminiKeyFailover'),('GeminiVideoCaptionClient.kt','GeminiKeyFailover'),('GeminiSubtitleClient.kt','GeminiKeyFailover')]:
    s=(base/filename).read_text()
    assert client in s and 'failover.run(' in s, filename
assert 'GeminiHttpException(status' in (base/'GeminiVideoCaptionClient.kt').read_text()
assert 'GeminiHttpException(code' in (base/'GeminiSubtitleClient.kt').read_text()
for filename in ('gemini_backup_rules.xml','gemini_data_extraction_rules.xml'):
    assert 'gemini_secret_local.xml' in (p/'app/src/main/res/xml'/filename).read_text()
assert 'v0.1.130' in (p/'AGENTS.md').read_text()
print('OK: 0.1.130 - multis credenciais, backup excluído, fallback por projeto e clientes integrados')
