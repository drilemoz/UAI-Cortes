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
    current_version, current_code = check(ROOT)
    assert current_version and current_code > 0
    stale_name = f'{current_version}.desatualizada'
    stale_code = current_code - 1
    cases = (
        ('github-manager.json', {'version': stale_name}),
        ('app_identity.json', {'version': stale_name}),
        ('github-manager.json', {'android': {'versionCode': stale_code}}),
        ('VERSION', f'{stale_name}\n'),
        ('.github/workflows/android-apk.yml', f"VERSION_CODE: '{stale_code}'"),
        ('app/src/main/java/br/com/uaicortes/AppReleaseNotes.kt',
            f'const val VERSION_NAME = "{stale_name}"'),
        ('tools/package_apks.py', f'VERSION_CODE = {stale_code}'),
    )
    for target, stale in cases:
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
                current_text = p.read_text()
                if target.endswith('.yml'):
                    old = f"VERSION_CODE: '{current_code}'"
                elif target.endswith('.kt'):
                    old = f'const val VERSION_NAME = "{current_version}"'
                else:
                    old = f'VERSION_CODE = {current_code}'
                assert old in current_text, f'Valor original não encontrado em {target}'
                p.write_text(current_text.replace(old, stale, 1))
            try:
                check(dst)
            except ValueError:
                pass
            else:
                raise AssertionError(f'Versão divergente não detectada: {target}')
    print(f'OK: validação sincronizada + {len(cases)} divergências detectadas')

if __name__ == '__main__':
    test()
