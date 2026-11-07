#!/usr/bin/env python3
"""v0.1.210: tela dividida por etapas, mantendo o mesmo fluxo real de divisão.
Validação de contrato não substitui o compilador nem testes de UI Android.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
ui = (src / 'QuickVideoToolsScreen.kt').read_text()
policy = (src / 'QuickVideoSplitPolicy.kt').read_text()
engine = (src / 'QuickVideoSplitEngine.kt').read_text()
for item in [
    'QuickToolStep(1, "Vídeo de origem")',
    'QuickToolStep(2, "Duração das partes")',
    'QuickToolStep(3, "Como salvar o áudio")',
    'onClick = { galleryOpen = true }',
    'QuickToolChoice(',
    'QuickDurationPreset("9,5 s",',
    'QuickDurationPreset("10 s",',
    'lastSecondsDuration by rememberSaveable',
    'QuickVideoSplitPolicy.TEN_SECONDS_MS',
    'Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())',
    'Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface)',
    'parts.take(5)',
    'if (previewExpanded.value)',
    'Modifier.fillMaxWidth().navigationBarsPadding()',
    'partDuration == option.milliseconds',
    'audioMode == "keep"', 'audioMode == "mute"', 'audioMode == "separate"',
    'enabled = !processing && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasAudio',
    'splitEngine.start(Uri.parse(sourceText), sourceName, parts, audioMode,',
    'audioDocument.launch(QuickVideoSplitPolicy.fileBase(sourceName) + ".m4a")',
    'galleryOpen -> galleryOpen = false',
    'processing -> confirmCancel = true',
    'splitView == SplitView.RESULT -> splitView = SplitView.SETUP',
    'splitEngine.cancel(); audioEngine.cancel()'
]:
    assert item in ui, item
assert ui.count('QuickToolChoice(') == 4
assert 'SHORT_MS = 9_500L' in policy and 'TEN_SECONDS_MS = 10_000L' in policy
assert 'MAX_PARTS = 180' in policy
assert 'setRemoveAudio(audioMode == "mute")' in engine
assert 'GalleryVideoSaver.save(context, output, fileName,' in engine
assert 'VideoProject(' not in ui
print('OK: cartões/etapas/rodapé e navegação por fases preservados')
