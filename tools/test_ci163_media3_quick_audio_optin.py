#!/usr/bin/env python3
"""CI163: evitar @UnstableApi na API pública do extrator de áudio.

Esta regressão estática não substitui o :app:lintDebug no CI.
"""
import re
from pathlib import Path
root = Path(__file__).resolve().parents[1]
engine = (root / "app/src/main/java/br/com/uaicortes/AudioExtractionEngine.kt").read_text()
screen = (root / "app/src/main/java/br/com/uaicortes/QuickVideoToolsScreen.kt").read_text()
split = (root / "app/src/main/java/br/com/uaicortes/QuickVideoSplitEngine.kt").read_text()
assert "import androidx.media3.common.util.UnstableApi" in engine
assert re.search(r'@androidx\.annotation\.OptIn\(UnstableApi::class\)\s+class\s+AudioExtractionEngine\(', engine)
assert not re.search(r'@UnstableApi\s+class\s+AudioExtractionEngine\(', engine)
for use in ('AudioExtractionEngine(context.applicationContext)', 'audioEngine.cancel()', 'audioEngine.start('):
    assert use in screen, f"Consumidor ausente: {use}"
assert '@androidx.annotation.OptIn(UnstableApi::class)' in split
app = (root / 'app/build.gradle.kts').read_text()
assert 'abortOnError = false' not in app
assert 'lint-baseline.xml' not in app
print('OK: CI163 - AudioExtractionEngine opt-in interno sem propagar UnstableApi para a tela rápida')
