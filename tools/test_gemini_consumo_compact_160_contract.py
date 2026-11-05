#!/usr/bin/env python3
"""Contratos v0.1.160: painel compacto, ajuda, tabela de preços conservadora.
Não substitui build Compose nem consulta oficial em tempo real.
"""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
p=r/'app/src/main/java/br/com/uaicortes'
u=(p/'GeminiUsageUi.kt').read_text()
a=(p/'GeminiPricingUi.kt').read_text()
c=(p/'GeminiPublicPriceCatalog.kt').read_text()
d=(p/'GeminiModelSettingsUi.kt').read_text()
assert 'GeminiAnalyticsSection(filtered, prices, period)' in u
assert 'GeminiModelSettingsDialog(project, configuringModel, quotas, prices' in u
assert 'GeminiUsageHelpButton' in u and 'Detalhes avançados' in u
assert 'models = entries.asSequence()' in u and 'listOf(GeminiModels.LITE' not in u
assert 'Adicionar modelo' in u and 'configuringModel = ""' in u
assert 'Agora:' in u and 'Hoje:' in u and 'Ver limites e créditos no Google' in u
assert 'stats.forEach { stat ->' in a
assert 'stat.success.toFloat() / stat.attempts' in a
assert 'progress = { stat.attempts.toFloat() / maxAttempts }' not in a
assert 'Gráficos e métricas' in a and 'GeminiUsageHelpButton' in a
assert 'GeminiPublicPriceCatalog.lookup(model)' in d
assert 'Confirm' not in d or 'freeConfirmed' in d
assert 'freeConfirmed' in d and 'GeminiManualPrice(0.0, 0.0)' in d
assert 'Preencher com preço de referência' in d
assert 'https://ai.google.dev/gemini-api/docs/pricing' in c
assert 'rate.validUntil' not in d  # data de vigência encapsulada no catálogo
assert '2027' in c and '2026' in c and 'gemini-3.5-transcribe' in c
assert 'Sem referência atual verificada' in d
assert 'quotas.save(project, model, nextLimits)' in d
assert 'prices.save(project, model, nextPrice)' in d
print('OK: 160 painel compacto, métrica correta, preços datados, confirmação do gratuito')
