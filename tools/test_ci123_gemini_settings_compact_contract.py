#!/usr/bin/env python3
"""Contrato estático CI123: layout Gemini sem regressão de segurança/uso.

Não verifica renderização Compose nem substitui o teste TTS/Android real.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
java = root/'app/src/main/java/br/com/uaicortes'
settings = (java/'GeminiSettingsUi.kt').read_text()
pool = (java/'GeminiKeyPoolSettings.kt').read_text()
usage = (java/'GeminiUsageUi.kt').read_text()

assert settings.count('GeminiKeyAction(') == 7
for title in ('Colar', 'Copiar', 'Importar', 'Exportar', 'Obter', 'Remover'):
    assert f'GeminiKeyAction("{title}"' in settings
assert 'BoxWithConstraints(Modifier.fillMaxWidth())' in settings
assert 'compactScreen = maxWidth < 312.dp' in settings
assert 'Modifier.horizontalScroll(rememberScrollState())' in settings
assert 'Modifier.weight(1f)' in settings
assert 'modifier.height(56.dp)' in settings
assert 'RoundedCornerShape(8.dp)' in settings
assert 'contentDescription = label' in settings
assert 'GeminiKeyAction("Remover", Icons.Default.DeleteOutline' in settings
assert 'dangerous = true) { confirm = "remove" }' in settings
assert 'confirm = "copy"' in settings
assert 'confirm = "export"' in settings
assert 'confirm = "test"' in settings
assert 'importPicker.launch(' in settings and 'exportPicker.launch(' in settings
assert 'store.save(key)' in settings and 'store.read()' in settings
assert 'ClipDescription.EXTRA_IS_SENSITIVE' in settings
assert 'visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation()' in settings
assert 'show = false' in settings
assert 'Somente o texto e os parâmetros da narração' in settings
assert 'GeminiKeyPoolSettings(store)' in settings

assert 'entries.size}/8 protegidas' in pool
assert 'store.setAutoFallback(true)' in pool
assert 'store.removeProfile(item.id)' in pool
assert 'Gerenciar chaves' in pool

for item in ('"Aceitas"', '"Tokens"', '"Avisos 429"'):
    assert item in usage
assert 'entries.filter { it.at >= cutoff }' in usage
assert 'filtered.count { it.status == "429" }' in usage
assert 'filtered.mapNotNull { it.tokens?.total }' in usage
assert 'filtered.count { it.status == "ok" && it.tokens?.total == null }' in usage
assert 'detailsExpanded' in usage and 'filtered.groupBy { it.project }' in usage
assert 'filtered.takeLast(5).asReversed()' in usage
assert 'ledger.clear()' in usage and 'confirmClear' in usage
assert 'Ver limites e créditos no Google' in usage
assert 'Não informa saldo de créditos da conta Google' in usage
assert 'Round' in usage and 'heightIn(min = 42.dp)' in usage
print('OK: barra Gemini compacta, segurança preservada e consumo local fiel')
