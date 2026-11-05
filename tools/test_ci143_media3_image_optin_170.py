#!/usr/bin/env python3
"""Regressao estatico do lintDebug do workflow 143; nao substitui o Android Lint."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
source = (root / 'app/src/main/java/br/com/uaicortes/StaticImageClip.kt').read_text()
assert 'import androidx.annotation.OptIn' in source
assert 'import androidx.media3.common.util.UnstableApi' in source
assert 'import kotlin.OptIn' not in source
assert re.search(r'@OptIn\(UnstableApi::class\)\s+fun playback\(clip: Clip\): MediaItem', source), 'OptIn local ausente'
assert 'setImageDurationMs(clip.durationMs)' in source
s = (root / 'app/build.gradle.kts').read_text()
assert 'abortOnError = false' not in s
assert 'lint-baseline.xml' not in s
print('OK: CI143 Media3 image opt-in local, Lint ativo')
