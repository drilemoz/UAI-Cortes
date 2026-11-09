#!/usr/bin/env python3
"""Regressão CI 209: ícone de informação válido na confirmação de exportação."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
source = (ROOT / "app/src/main/java/br/com/uaicortes/ExportScreen.kt").read_text(encoding="utf-8")
assert "Icons.Default.InfoOutline" not in source, "CI 209: referência inválida ao ícone InfoOutline voltou"
assert re.search(r"Icon\(Icons\.Default\.Info,\s*null,", source), "O ícone informativo precisa ser Icons.Default.Info"
assert "Mantenha o aplicativo aberto." in source
assert "sem publicar automaticamente" in source
assert "import androidx.compose.material.icons.filled.*" in source or "import androidx.compose.material.icons.filled.Info" in source
print("OK: CI 209, ícone de informação compilável e mensagem de exportação preservada")
