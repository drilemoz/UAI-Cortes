#!/usr/bin/env python3
"""Contratos de responsividade e acessibilidade, sem substituir testes Android."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
ui = (root / 'app/src/main/java/br/com/uaicortes/QuickVideoToolsScreen.kt').read_text()
engine = (root / 'app/src/main/java/br/com/uaicortes/QuickVideoSplitEngine.kt').read_text()
policy = (root / 'app/src/main/java/br/com/uaicortes/QuickVideoSplitPolicy.kt').read_text()
assert 'BoxWithConstraints(Modifier.fillMaxWidth())' in ui
assert 'maxWidth < 330.dp || LocalDensity.current.fontScale >= 1.3f' in ui
assert 'QuickToolDurationChoice("9,5 s"' in ui and 'QuickToolDurationChoice("10 s"' in ui
assert 'Modifier.weight(1f)' in ui and 'Modifier.fillMaxWidth()' in ui
assert 'stateDescription = if (selected) "Selecionado" else "Não selecionado"' in ui
assert 'role = Role.RadioButton' in ui
assert 'LiveRegionMode.Polite' in ui
assert 'Parte $progressIndex de $progressTotal' in ui
assert 'LinearProgressIndicator(modifier = Modifier.fillMaxWidth())' in ui
assert 'SHORT_MS = 9_500L' in policy and 'TEN_SECONDS_MS = 10_000L' in policy
assert 'MAX_PARTS = 180' in policy
assert 'splitEngine.start(Uri.parse(sourceText), sourceName, parts, audioMode,' in ui
assert 'onClick = { galleryOpen = true }' in ui
assert 'AudioExtractionEngine(context.applicationContext)' in ui
assert 'GalleryVideoSaver.save(context, output, fileName, subfolder = "Divididos")' in engine
print('OK: responsividade/acessibilidade e contratos do corte preservados')
