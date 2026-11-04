#!/usr/bin/env python3
"""Regressões comprovadas nos logs do workflow 110.

Contrato fonte: não substitui :app:compileDebugKotlin e :app:compileReleaseKotlin.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
ui = (root / "app/src/main/java/br/com/uaicortes/GeminiUsageUi.kt").read_text()
client = (root / "app/src/main/java/br/com/uaicortes/GeminiVideoCaptionClient.kt").read_text()
assert re.search(r"(?m)^import androidx\.compose\.foundation\.horizontalScroll\s*$", ui), "Falta o import real de horizontalScroll"
assert "Modifier.horizontalScroll(rememberScrollState())" in ui
assert "import androidx.compose.foundation.rememberScrollState" in ui
assert re.search(r"private suspend fun countInput\([^)]*\): Long = withContext\(Dispatchers\.IO\)\s*\{",client)
assert 'return@withContext result.getLong("totalTokens").coerceAtLeast(0L)' in client
assert not re.search(r'(?m)^\s*return\s+result\.getLong\("totalTokens"\)', client)
assert ":countTokens" in client and "requestJsonOnce" in client
assert (root / "VERSION").read_text().strip() == "0.1.133"
print("OK: CI110 — import horizontalScroll e retorno rotulado em withContext; countTokens sob demanda preservado")
