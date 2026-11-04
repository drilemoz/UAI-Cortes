#!/usr/bin/env python3
"""CI117: garante que o empacotador de fonte não omite .github/workflows."""
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root/'tools'))
from check_version_sync import check
from package_source import REQUIRED, SOURCE_DIRS, checked_files

workflow = '.github/workflows/android-apk.yml'
assert '.github' in SOURCE_DIRS, 'O diretório oculto .github deve ser empacotado'
assert workflow in REQUIRED, 'O workflow deve ser obrigatório no ZIP'
files = {path.relative_to(root).as_posix() for path in checked_files()}
assert workflow in files, 'Workflow excluído ao selecionar arquivos para o ZIP'
version, code = check(root)
assert version and code >= 141
assert (root/'tools/package_source.py').is_file()
print(f'OK: CI117 fonte inclui {workflow} e versão {version} (+ {len(files)} arquivos).')
