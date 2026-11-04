#!/usr/bin/env python3
"""Previne regressão do workflow 115: testes históricos não fixam versão corrente."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
test = (root / "tools/test_subtitle_karaoke_136_contract.py").read_text(encoding="utf-8")
assert "from check_version_sync import check" in test
assert "version, code=check(root)" in test
assert "version=='0.1.136'" not in test
assert "code==137" not in test
assert "tuple(map(int, version.split('.'))) >= (0, 1, 136)" in test
assert "code >= 137" in test

# Garante a detecção de outros testes históricos com versão e código fixos.
for path in sorted((root / "tools").glob("test_*_contract.py")):
    if path.name == "test_ci115_karaoke_version_contract.py":
        continue
    content = path.read_text(encoding="utf-8")
    matches = re.findall(r"assert\s+version\s*==\s*['\"]0\.1\.\d+['\"]\s+and\s+code\s*==\s*\d+", content)
    assert not matches, f"Teste histórico com versão fixa: {path.name}"

print("OK: CI115 sem versão fixa no teste do karaokê e demais contratos.")
