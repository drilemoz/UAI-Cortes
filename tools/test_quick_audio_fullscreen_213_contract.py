#!/usr/bin/env python3
"""0.1.213: contrato estático para extrair áudio com execução e resultado em tela inteira.
Não equivale a um build Android nem a teste instrumentado Media3/SAF.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
main = root/'app/src/main/java/br/com/uaicortes'
ui = (main/'QuickVideoToolsScreen.kt').read_text()
flow = (main/'QuickAudioSessionScreen.kt').read_text()
engine = (main/'AudioExtractionEngine.kt').read_text()
for token in (
    'ActivityResultContracts.CreateDocument("audio/mp4")',
    'if (uri != null && sourceText.isNotBlank() && hasAudio && !processing)',
    'splitView = SplitView.RUNNING',
    'mode == QuickToolMode.EXTRACT_AUDIO && splitView == SplitView.RUNNING',
    'mode == QuickToolMode.EXTRACT_AUDIO && splitView == SplitView.RESULT',
    'QuickAudioProgressScreen(', 'QuickAudioResultScreen(',
    'onPhase = { step ->', 'extractedAudioUri = uri',
    'audioEngine.cancel()', '"Extração cancelada pelo usuário."',
    'setDataAndType(uri, "audio/mp4")', 'Intent.FLAG_GRANT_READ_URI_PERMISSION',
    '"Nova extração"' if False else 'onNew = {',
): assert token in ui, token
assert 'if (processing && splitView == SplitView.RUNNING)' in ui
assert ui.index('extractedAudioUri = uri') > ui.index('onSuccess = {')
for token in (
    'fun QuickAudioProgressScreen(', 'fun QuickAudioResultScreen(',
    'CircularProgressIndicator(', 'LinearProgressIndicator(modifier = Modifier.fillMaxWidth())',
    '"Cancelar extração"', '"Áudio extraído com sucesso"',
    '"Nova extração"', '"Ouvir áudio"', '"Nenhum M4A concluído foi confirmado',
    'onOpen(savedUri!!)',
): assert token in flow, token
assert 'savedUri != null && issue.isBlank()' in flow
assert 'progress = {' not in flow # indeterminate, no internal percentages
for token in (
    'onPhase: (String) -> Unit = {}',
    'onPhase("Extraindo faixa de áudio")',
    'onPhase("Salvando arquivo M4A")',
    'private val generation = AtomicInteger(0)',
    'generation.incrementAndGet()',
    'stoppedCopyWorker = copyWorker?.also { it.interrupt() }',
    'if (!isCurrent()) return@post',
    'openOutputStream(destination, "w")',
    'result.fold({ onSuccess() }',
): assert token in engine, token
assert engine.index('out.flush()') < engine.index('result.fold({ onSuccess() }')
print('OK: v213 tela completa SAF + etapas reais, conclusão por URI, cancelamento protegido')
