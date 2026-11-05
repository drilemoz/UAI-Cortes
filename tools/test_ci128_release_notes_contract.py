#!/usr/bin/env python3
"""CI128: publicação pequena preservando o histórico integral."""
from pathlib import Path
from tempfile import TemporaryDirectory
from release_notes_for_github import render, main, ROOT, MAX_PUBLICATION_BYTES
import os
import subprocess
import sys


def test() -> None:
    version = (ROOT/'VERSION').read_text().strip()
    raw = (ROOT/'RELEASE_NOTES.md').read_text(encoding='utf-8')
    notes = render(raw, version, 'drilemoz/UAI-Cortes')
    assert notes.startswith(f'# Uai Cortes · versão {version}')
    assert len(notes.encode('utf-8')) <= MAX_PUBLICATION_BYTES < 125000
    assert 'CI128' in notes
    assert 'blob/main/RELEASE_NOTES.md' in notes
    assert len(raw) > len(notes) and '0.1.151' in raw
    script = (ROOT/'tools/release_notes_for_github.py')
    workflow = (ROOT/'.github/workflows/android-apk.yml').read_text()
    assert 'python3 tools/release_notes_for_github.py' in workflow
    assert '--notes-file dist/RELEASE_NOTES_GITHUB.md' in workflow
    assert '--notes-file RELEASE_NOTES.md' not in workflow
    assert 'gh release edit "$TAG" --notes-file dist/RELEASE_NOTES_GITHUB.md' in workflow
    assert script.is_file()
    try:
        render('# Uai Cortes · versão 0.1.151\n\nAntiga.\n', version)
    except ValueError:
        pass
    else:
        raise AssertionError('Versão incorreta não rejeitada')
    huge = f'# Uai Cortes · versão {version}\n\n' + 'Café ☕ — teste de UTF-8.\n'*10000 + '# Uai Cortes · versão 0.1.151\n\nHistórico antigo'
    small = render(huge, version)
    assert len(small.encode('utf-8')) <= MAX_PUBLICATION_BYTES
    assert 'Histórico antigo' not in small
    assert 'Notas encurtadas' in small
    with TemporaryDirectory() as tmp:
        output = Path(tmp)/'release.md'
        subprocess.run([sys.executable, str(script), '--output', str(output)], check=True, cwd=ROOT, capture_output=True)
        assert output.read_text(encoding='utf-8')
        assert output.stat().st_size <= MAX_PUBLICATION_BYTES
    print('OK: CI128 — notas do GitHub menores que limite, UTF-8, histórico completo, reuso de Release')

if __name__ == '__main__':
    test()
