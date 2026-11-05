#!/usr/bin/env python3
"""Regressão estática da reformulação Gemini v0.1.148.
Não substitui compilação Compose, acessibilidade ou teste Android real.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
base=root/'app/src/main/java/br/com/uaicortes'
settings=(base/'GeminiSettingsUi.kt').read_text()
pool=(base/'GeminiKeyPoolSettings.kt').read_text()
usage=(base/'GeminiUsageUi.kt').read_text()
assert 'Gemini · Conexão de IA' in settings
assert 'Credencial ativa' in settings and 'Nova chave da API' in settings
assert 'Ações da chave' in settings
for action in ('Colar','Copiar','Importar','Exportar','Obter','Remover'):
    assert f'GeminiKeyAction("{action}"' in settings
assert 'ClipDescription.EXTRA_IS_SENSITIVE' in settings
assert 'confirm = "copy"' in settings and 'confirm = "export"' in settings and 'confirm = "remove"' in settings
assert 'confirm = "test"' in settings and 'GeminiKeyPoolSettings(store)' in settings
assert 'formOpen' in pool and 'Dialog(onDismissRequest' in pool
assert 'Adicionar chave Gemini' in pool and 'Editar chave Gemini' in pool
assert 'Text("Usar"' not in pool or 'store.select(profile.id)' in pool
for term in ('store.add(secret, label, project)', 'store.rename(editing!!.id, label, project)',
             'store.removeProfile(item.id)', 'store.setAutoFallback(true)', 'confirmAuto',
             'store.temporarilyBlocked(profile.projectId, it)', 'Nenhuma tentativa ocorre em segundo plano.'):
    assert term in pool, term
assert 'entries.size}/8 protegidas' in pool
assert 'Consumo da IA' in usage and 'Histórico local' in usage
for term in ('"Aceitas"', '"Tokens"', '"Avisos 429"',
             'filtered.count { it.status == "429" }',
             'filtered.mapNotNull { it.tokens?.total }', 'ledger.clear()',
             'Ver limites e créditos no Google'):
    assert term in usage, term
assert tuple(map(int, (root/'VERSION').read_text().strip().split('.'))) >= (0, 1, 148)
print('OK: tela Gemini, gerenciamento em modal, segurança e consumo preservados v0.1.148')
