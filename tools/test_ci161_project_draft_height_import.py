#!/usr/bin/env python3
"""Regressão CI161: usos de Modifier.height requerem import Compose explícito.

Contrato estático: não substitui o compilador Kotlin/Gradle.
"""
from pathlib import Path
import re

source = (Path(__file__).resolve().parents[1]
          / "app/src/main/java/br/com/uaicortes/ProjectDraftUi.kt").read_text(encoding="utf-8")
assert source.count("import androidx.compose.foundation.layout.height") == 1, (
    "ProjectDraftUi.kt: importar androidx.compose.foundation.layout.height"
)
assert "Modifier.weight(1f).height(76.dp)" in source
assert "Modifier.height(22.dp)" in source
assert len(re.findall(r"\bModifier\.[^\n]*\bheight\(", source)) >= 2
assert 'EditorExitAction("Salvar"' in source
assert 'EditorExitAction("Descartar"' in source
assert 'EditorExitAction("Continuar"' in source
print("OK CI161: import Compose height presente; ações horizontais do editor preservadas")
