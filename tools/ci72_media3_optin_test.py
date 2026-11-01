#!/usr/bin/env python3
"""Regressão do CI 72: uso explícito e LOCAL de APIs Media3 instáveis.

Isto é verificação de fonte, não substitui Android Lint real.
"""
import re
from pathlib import Path
root = Path(__file__).resolve().parents[1]
file = root / 'app/src/main/java/br/com/uaicortes/VideoEffects.kt'
s = file.read_text(encoding='utf-8')
assert 'import androidx.annotation.OptIn' in s
assert 'import kotlin.OptIn' not in s
assert 'import androidx.media3.common.util.UnstableApi' in s
for cls in ('SubtitleOutlineSpan','SubtitleShadowSpan'):
    pattern = rf'@OptIn\(UnstableApi::class\)\s+class\s+{cls}\s*\('
    assert re.search(pattern, s), f'{cls}: falta @androidx.annotation.OptIn'
    part = s.split('class '+cls+'(',1)[1].split('\nclass ',1)[0].split('\n@UnstableApi',1)[0]
    assert 'TextOverlay.TEXT_SIZE_PIXELS' in part, f'{cls}: ausência do uso esperado'
# A única finalidade é tratar os locais sinalizados; não ocultar os demais diagnósticos.
gradle = (root/'app/build.gradle.kts').read_text(encoding='utf-8')
assert 'abortOnError = false' not in gradle
assert 'lint-baseline.xml' not in gradle
print('OK: CI72 -- os dois spans declaram OptIn AndroidX e Lint permanece ativo')
