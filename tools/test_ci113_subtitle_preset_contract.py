#!/usr/bin/env python3
"""CI113: presets de legendas são identificados por nome, não pela ordem da lista.
O compilador e os testes JVM do GitHub Actions continuam sendo a fonte da validação final.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
test = (root/'app/src/test/java/br/com/uaicortes/SubtitleStylesTest.kt').read_text(encoding='utf-8')
styles = (root/'app/src/main/java/br/com/uaicortes/SubtitleStyles.kt').read_text(encoding='utf-8')
assert 'first { it.name == "Etiqueta" }' in test, 'Modelo Etiqueta deve ser identificado pelo nome'
assert not re.search(r'SubtitleStyles\.presets\.last\(\)', test), 'Teste presume que o último estilo tem identidade fixa'
assert 'everyPresetPreservesContentTimingPositionAndSizeRegardlessOfOrdering' in test
assert 'for (preset in presets.reversed())' in test
assert 'SubtitleStylePreset("Etiqueta"' in styles, 'Modelo esperado foi removido'
assert 'SubtitleStylePreset("Ritmo rosa"' in styles, 'Novo modelo deve ser preservado'
from check_version_sync import check
version, version_code=check(root)
assert version_code >= 136
print(f'OK: CI113 — preset por nome, regressão com catálogo reordenado e v{version}')
