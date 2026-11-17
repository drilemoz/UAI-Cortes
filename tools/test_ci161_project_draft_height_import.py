#!/usr/bin/env python3
"""Shared compact actions must preserve both Compose dimensions and exit affordances."""
from pathlib import Path
src = Path(__file__).resolve().parents[1] / "app/src/main/java/br/com/uaicortes"
dialog = (src / "ProjectDraftUi.kt").read_text()
tile = (src / "CompactActionTile.kt").read_text()
assert "import androidx.compose.foundation.layout.*" in tile
assert "Modifier.weight(1f).heightIn(min = 58.dp)" in tile
assert "Modifier.size(18.dp)" in tile
for action in ("Salvar", "Rascunho", "Descartar", "Editar"):
    assert f'CompactActionTile("{action}"' in dialog
print("OK CI161: dimensões Compose e ações horizontais do editor")
