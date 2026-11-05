#!/usr/bin/env python3
"""Contratos da previsão local e troca controlada (não substitui build/Google real)."""
from pathlib import Path
p=Path(__file__).resolve().parents[1]
s=p/'app/src/main/java/br/com/uaicortes'
forecast=(s/'GeminiQuotaForecast.kt').read_text()
quota_ui=(s/'GeminiQuotaUi.kt').read_text()
usage=(s/'GeminiUsageUi.kt').read_text()
fail=(s/'GeminiKeyFailover.kt').read_text()
store=(s/'GeminiCredentialStore.kt').read_text()
assert 'America/Los_Angeles' in forecast
assert 'averageTotal' in forecast and 'averageInput' in forecast
assert 'totals.size >= 5' in forecast and 'inputs.size >= 5' in forecast
assert 'it.project == project && it.model == model' in forecast
assert 'GeminiManualLimits' in forecast and 'remainingByLocalLimits' in forecast
assert 'limits.tpmInput' in forecast and 'it.tokens?.input' in forecast
assert 'getSharedPreferences("gemini_limits_local_v1"' in forecast
assert 'SHA-256' in forecast and 'project + "\\n" + model' in forecast
assert 'GeminiQuotaBudgetDialog' in usage and 'GeminiQuotaBudgetDialog' in quota_ui
assert 'GeminiUsageHint(' in usage and 'requestedCount' in usage
assert 'GeminiQuotaPolicy.compute' in usage
assert 'GeminiUsageHint("narração"' in (s/'VoiceIaScreen.kt').read_text()
assert 'requestedCount = count' in (s/'VoiceIaScreen.kt').read_text()
assert 'store.autoFallbackEnabled()' in fail
assert 'store.markExhausted(candidate.projectId, model)' in fail
assert 'if (!quota429(error)) throw error' in fail
assert 'store.temporarilyBlocked(candidate.projectId, model)' in fail
assert 'setAutoFallback(true)' in (s/'GeminiKeyPoolSettings.kt').read_text()
assert 'model: String = ""' in fail and 'model: String = ""' in store
assert all('gemini_limits_local_v1.xml' in (p/'app/src/main/res/xml'/x).read_text() for x in ('gemini_backup_rules.xml','gemini_data_extraction_rules.xml'))
assert 'GeminiQuotaPolicyTest' in (p/'app/src/test/java/br/com/uaicortes/GeminiQuotaPolicyTest.kt').read_text()
print('OK: previsão por função/modelo/projeto, configuração manual, failover 429 por modelo e privacidade')
