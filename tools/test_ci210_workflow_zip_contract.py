#!/usr/bin/env python3
"""CI210: regressão da entrega incompleta, que ignorava .github no ZIP publicado."""
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'tools'))
from check_version_sync import check
from package_source import build_zip

version, code = check(ROOT)
with TemporaryDirectory(prefix='uaicortes-ci210-') as temp:
    target = Path(temp) / f'UaiCortes-v{version}-fonte.zip'
    build_zip(target, version)
    with ZipFile(target) as archive:
        names = set(archive.namelist())
        workflow_name = '.github/workflows/android-apk.yml'
        assert workflow_name in names, 'ZIP perdeu o diretório .github/workflows'
        workflow = archive.read(workflow_name).decode('utf-8')
        assert f"VERSION: '{version}'" in workflow
        assert f"VERSION_CODE: '{code}'" in workflow
        assert '.github/workflows/android-apk.yml' in names
        assert 'app/build.gradle.kts' in names
        assert 'tools/check_version_sync.py' in names
        assert 'tools/package_source.py' in names
        assert not any(n.startswith('.git/') for n in names)
        assert not any(n.endswith(('.p12', '.keystore', '.jks', '.key', '.env')) for n in names)
print(f'OK CI210: ZIP real inclui .github e workflow v{version}/versionCode {code}')
