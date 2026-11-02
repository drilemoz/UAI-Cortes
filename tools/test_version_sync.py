#!/usr/bin/env python3
"""Testes negativos: uma única referência desatualizada deve reprovar o CI."""
from pathlib import Path
import json
import shutil
from tempfile import TemporaryDirectory
from check_version_sync import check, ROOT

FILES = [
    'app/build.gradle.kts', '.github/workflows/android-apk.yml',
    'app/src/main/java/br/com/uaicortes/AppReleaseNotes.kt',
    'tools/package_apks.py', 'github-manager.json', 'app_identity.json', 'VERSION',
    'README.md', 'CHANGELOG.md', 'RELEASE_NOTES.md',
]

def test() -> None:
    assert check(ROOT) == ('0.1.99', 100)
    for target, stale in (
        ('github-manager.json', {'version': '0.1.98'}),
        ('app_identity.json', {'version': '0.1.98'}),
        ('github-manager.json', {'android': {'versionCode': 99}}),
        ('VERSION', '0.1.98\n'),
        ('.github/workflows/android-apk.yml', "VERSION_CODE: '99'"),
    ):
        with TemporaryDirectory() as d:
            dst = Path(d)
            for file in FILES:
                (dst/file).parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(ROOT/file, dst/file)
            p = dst/target
            if isinstance(stale, dict):
                document = json.loads(p.read_text())
                for key, value in stale.items():
                    if isinstance(value, dict): document[key].update(value)
                    else: document[key] = value
                p.write_text(json.dumps(document))
            elif target == 'VERSION':
                p.write_text(stale)
            else:
                p.write_text(p.read_text().replace("VERSION_CODE: '100'", stale))
            try:
                check(dst)
            except ValueError:
                pass
            else:
                raise AssertionError(f'Versão divergente não detectada: {target}')
    print('OK: validação sincronizada + 5 divergências detectadas')

if __name__ == '__main__':
    test()
