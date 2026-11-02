#!/usr/bin/env python3
"""Verifica divergências entre a versão Android, GitHub Manager, Home/Sobre e CI.

A leitura parte do Gradle: a versão esperada não é duplicada neste script.
"""
from __future__ import annotations
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]


def require_regex(content: str, pattern: str, source: str) -> str:
    match = re.search(pattern, content, re.MULTILINE)
    if not match:
        raise ValueError(f'{source}: campo obrigatório não encontrado: {pattern}')
    return match.group(1)


def check(root: Path = ROOT) -> tuple[str, int]:
    root = Path(root)
    android = (root/'app/build.gradle.kts').read_text(encoding='utf-8')
    version = require_regex(android, r'versionName\s*=\s*"([^"\n]+)"', 'app/build.gradle.kts')
    code = int(require_regex(android, r'versionCode\s*=\s*(\d+)', 'app/build.gradle.kts'))
    app_id = require_regex(android, r'applicationId\s*=\s*"([^"\n]+)"', 'app/build.gradle.kts')
    compile_sdk = int(require_regex(android, r'compileSdk\s*=\s*(\d+)', 'app/build.gradle.kts'))
    target_sdk = int(require_regex(android, r'targetSdk\s*=\s*(\d+)', 'app/build.gradle.kts'))
    def equals(value: object, expected: object, location: str) -> None:
        if value != expected:
            raise ValueError(f'{location}: esperado {expected!r}; encontrado {value!r}')

    for name in ('github-manager.json', 'app_identity.json'):
        data = json.loads((root/name).read_text(encoding='utf-8'))
        equals(data.get('displayName'), 'Uai Cortes', name+'.displayName')
        equals(data.get('version'), version, name+'.version')
        android_data = data.get('android')
        if not isinstance(android_data, dict):
            raise ValueError(f'{name}: android ausente')
        equals(android_data.get('applicationId'), app_id, name+'.android.applicationId')
        equals(android_data.get('versionName'), version, name+'.android.versionName')
        equals(android_data.get('versionCode'), code, name+'.android.versionCode')
        if name == 'github-manager.json':
            equals(android_data.get('compileSdk'), compile_sdk, name+'.android.compileSdk')
            equals(android_data.get('targetSdk'), target_sdk, name+'.android.targetSdk')
    equals((root/'VERSION').read_text().strip(), version, 'VERSION')
    workflow = (root/'.github/workflows/android-apk.yml').read_text()
    equals(require_regex(workflow, r"VERSION:\s*'([^']+)'", 'workflow'), version, 'workflow.VERSION')
    equals(int(require_regex(workflow, r"VERSION_CODE:\s*'(\d+)'", 'workflow')), code, 'workflow.VERSION_CODE')
    release = (root/'app/src/main/java/br/com/uaicortes/AppReleaseNotes.kt').read_text()
    equals(require_regex(release, r'const val VERSION_NAME\s*=\s*"([^"]+)"', 'AppReleaseNotes.kt'), version, 'Sobre.VERSION_NAME')
    equals(int(require_regex(release, r'const val VERSION_CODE\s*=\s*(\d+)', 'AppReleaseNotes.kt')), code, 'Sobre.VERSION_CODE')
    equals(require_regex(release, r'ReleaseSummary\("([^"\n]+)"', 'AppReleaseNotes.kt'), version, 'Novidades.versão atual')
    package = (root/'tools/package_apks.py').read_text()
    equals(require_regex(package, r"^VERSION\s*=\s*'([^']+)'", 'package_apks.py'), version, 'package_apks.VERSION')
    equals(int(require_regex(package, r'^VERSION_CODE\s*=\s*(\d+)', 'package_apks.py')), code, 'package_apks.VERSION_CODE')
    for name in ('README.md', 'CHANGELOG.md', 'RELEASE_NOTES.md'):
        header = (root/name).read_text(encoding='utf-8').splitlines()[0]
        if version not in header:
            raise ValueError(f'{name}: cabeçalho sem a versão {version}')
    return version, code


if __name__ == '__main__':
    try:
        root = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT
        version, code = check(root)
        print(f'OK: v{version} (versionCode {code}): Gradle, Sobre, Novidades, GitHub Manager, ZIP e CI em sincronia')
    except (ValueError, OSError, json.JSONDecodeError) as exc:
        print(f'ERRO de versão: {exc}', file=sys.stderr)
        sys.exit(1)
