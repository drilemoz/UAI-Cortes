#!/usr/bin/env python3
"""CI221: impede regressão de import Compose Surface nos diálogos de rascunho.

O compilador Kotlin só resolve Surface com import explícito; análise sintática
não detecta esse erro. Este contrato complementa, mas não substitui, o Gradle.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
source = (root / 'app/src/main/java/br/com/uaicortes/ProjectDraftUi.kt').read_text(encoding='utf-8')
required_import = 'import androidx.compose.material3.Surface'
assert required_import in source.splitlines(), 'CI221: falta import de androidx.compose.material3.Surface'
assert source.count('Surface(') >= 3, 'CI221: cartões Surface de rascunho ausentes'
assert 'fun ConfirmRecoverDraft(' in source and 'fun InvalidEditorDraft(' in source
assert re.search(r'@Composable\s+internal fun ConfirmRecoverDraft', source)
assert re.search(r'@Composable\s+internal fun InvalidEditorDraft', source)
print('OK CI221: import Surface explícito e diálogos de rascunho mantidos')
