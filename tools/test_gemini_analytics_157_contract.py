#!/usr/bin/env python3
"""Regressão estrutural para custos opcionais e latência real, sem inventar uso/cobrança."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
P = ROOT / 'app/src/main/java/br/com/uaicortes'
analytics = (P/'GeminiUsageAnalytics.kt').read_text()
ui = (P/'GeminiUsageUi.kt').read_text()
prices = (P/'GeminiPricingUi.kt').read_text()
ledger = (P/'GeminiUsageLedger.kt').read_text()
assert 'GeminiManualPrice' in analytics and 'price.complete()' in analytics
assert 'usage.input ?: return null' in analytics and 'usage.output ?: return null' in analytics
assert 'if (event.status != "ok"' in analytics
assert 'it.status == "ok"' in analytics and 'sample.size >= 5' in analytics
assert 'if (measured.isNotEmpty())' in analytics
assert 'GeminiAnalyticsSection(filtered, prices, period)' in ui
assert 'GeminiModelSettingsDialog(project, configuringModel, quotas, prices' in ui
assert '"Configurar"' in ui and 'GeminiUsageHelpButton' in ui
assert 'Linha' not in prices
assert 'Tarifas podem variar' in prices and 'não representam fatura' in (P/'GeminiModelSettingsUi.kt').read_text().lower()
assert 'latencyMs' in ledger and 'GeminiUsageEvent(' in ledger
for name in ('GeminiTtsCore.kt', 'GeminiVideoCaptionClient.kt', 'GeminiSubtitleClient.kt'):
    code = (P/name).read_text()
    assert 'System.nanoTime()' in code and 'latencyMs = elapsedMs()' in code, name
print('OK: Gemini 157 preço manual, custo parcial, gráficos, tempo real e testes')
