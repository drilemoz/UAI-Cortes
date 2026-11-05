#!/usr/bin/env python3
"""Separa cotas temporais e conserva áudio já pronto em geração em lote."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
p=root/'app/src/main/java/br/com/uaicortes'
forecast=(p/'GeminiQuotaForecast.kt').read_text()
usage=(p/'GeminiUsageUi.kt').read_text()
voice=(p/'VoiceIaScreen.kt').read_text()
pacing=(p/'GeminiLocalPacing.kt').read_text()
planner=(p/'VoiceIaBatchPlanner.kt').read_text()
assert all(t in forecast for t in ['remainingNow','remainingToday','remainingByRpm','remainingByTpm','nextRpmWindowMs', 'America/Los_Angeles'])
assert 'rpmWaitMs(' in forecast and 'oldestToExpire' in forecast
assert 'lastMinute.all { it.tokens?.input != null }' in forecast
assert 'remainingByLocalLimits' in forecast
assert 'Hoje (Pacífico)' in usage and 'Agora:' in usage
assert 'GeminiLocalPacing(context)' in voice
assert 'pacing.beforeRequest(usedModel)' in voice
assert 'delay(waitMs)' in pacing and 'coroutineContext.ensureActive()' in pacing
assert 'limits.rpd != null && forecast.remainingToday == 0' in pacing
assert 'VoiceIaBatchPlanner.plan(' in voice and 'VoiceIaBatchItem' in planner
assert 'File(it.filePath).isFile' in voice
assert 'Nenhuma palavra será removida' in voice
assert 'GeminiQuotaWindowTest' in (root/'app/src/test/java/br/com/uaicortes/GeminiQuotaWindowTest.kt').read_text()
print('OK: RPM e RPD separados, ritmo local e plano de reaproveitamento de WAV')
