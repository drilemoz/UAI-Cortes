#!/usr/bin/env python3
"""Contrato estrutural complementar aos testes puros Kotlin da v0.1.116."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
ui=(r/'app/src/main/java/br/com/uaicortes/GeminiNarrationUi.kt').read_text()
ref=(r/'app/src/main/java/br/com/uaicortes/AudioReferenceAlignmentUi.kt').read_text()
logic=(r/'app/src/main/java/br/com/uaicortes/NarrationGenerationEligibility.kt').read_text()
assert 'activeTimedLine?.let { line ->' in ui and 'Fala ${activeTimedIndex + 1} de ${timedLines.size}' in ui
assert 'timedLines.forEach { line ->' not in ui
assert ui.index('HorizontalDivider()\n                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp') > ui.index('Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(narrationScrollState)')
assert 'enabled = generationIssues.isEmpty()' in ui
assert 'generationIssues.forEach { reason ->' in ui
assert 'Text("Consultar vozes"' in ui or '"Consultar vozes" else "Escolher voz"' in ui
assert 'items(visibleReuseLines, key = { it.id })' in ui
assert 'Text("Selecionar esta página")' in ui and 'Text("Próximas 12")' in ui
assert 'segments.forEach { segment ->' not in ref
assert 'selectedSegment?.let { segment ->' in ref and 'Fala ${index + 1} de ${segments.size}' in ref
assert 'ReferenceAudioProbe.analyze(context, slices' in ref and '"Aplicar horários"' in ref
assert 'api.synthesize' not in logic and 'blockingReasons' in logic
assert (r/'app/src/test/java/br/com/uaicortes/NarrationGenerationEligibilityTest.kt').exists()
print('OK: v0.1.116 navegação compacta, diagnóstico do botão e análise manual preservada')
