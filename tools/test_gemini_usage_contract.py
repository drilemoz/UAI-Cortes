#!/usr/bin/env python3
"""Contratos estáticos do monitor Gemini (não substituem Android/API real)."""
from pathlib import Path
p=Path(__file__).resolve().parents[1]
s=p/'app/src/main/java/br/com/uaicortes'
ledger=(s/'GeminiUsageLedger.kt').read_text()
ui=(s/'GeminiUsageUi.kt').read_text()
settings=(s/'GeminiSettingsUi.kt').read_text()
caption=(s/'GeminiVideoCaptionClient.kt').read_text()
tts=(s/'GeminiTtsCore.kt').read_text()
asr=(s/'GeminiSubtitleClient.kt').read_text()
assert 'usageMetadata' in ledger and 'totalTokenCount' in ledger
assert '"usage"' in ledger and 'total_tokens' in ledger
assert 'total_cached_tokens' in ledger and 'thoughtsTokenCount' in ledger
assert '90L * 24L' in ledger and 'gemini_usage_local_v1' in ledger
assert 'safeRecord' in ledger and 'storageLock' in ledger
assert all(x not in ledger for x in ['put("prompt"','put("secret"','put("audio"','put("apiKey"'])
for name in ('narracao','legendas','descricao','flow'):
    assert name in tts+asr+caption
assert 'safeRecord' in tts and 'safeRecord' in asr and 'safeRecord' in caption
assert 'status = if (status == 429)' in caption
assert 'GeminiUsagePanel()' in settings
assert 'GeminiUsageHint(' in ui
assert 'Consumo da IA' in ui and 'Ver limites e créditos no Google' in ui
assert 'countCaptionInput' in caption and 'countFlowInput' in caption
assert ':countTokens' in caption and 'requestJsonOnce' in caption
assert 'Conferir tokens de entrada' in (s/'VideoSavedDialog.kt').read_text()
assert 'Conferir tokens de entrada' in (s/'FlowPromptScreen.kt').read_text()
for file in ('VideoSavedDialog.kt','FlowPromptScreen.kt','AutoSubtitleScreen.kt','GeminiNarrationUi.kt'):
    assert 'GeminiUsageHint(' in (s/file).read_text(),file
assert 'Não informa saldo de créditos' in ui
for name in ('gemini_backup_rules.xml','gemini_data_extraction_rules.xml'):
    assert 'gemini_usage_local_v1.xml' in (p/'app/src/main/res/xml'/name).read_text()
assert 'GeminiUsageLedgerAndroidTest' in (p/'app/src/androidTest/java/br/com/uaicortes/GeminiUsageLedgerAndroidTest.kt').read_text()
print('OK: histórico local, dados reais, estimativas explícitas e pré-contagem opcional')
