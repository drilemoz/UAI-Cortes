#!/usr/bin/env python3
"""Gera e valida um ZIP de código-fonte completo do Uai Cortes.

Uso: python3 tools/package_source.py --output /caminho/UaiCortes-vX.Y.Z.zip
Importante: nunca adiciona chaves, configurações locais, binários de build ou cache.
"""
import argparse
from pathlib import Path
import re
import subprocess
import sys
from tempfile import TemporaryDirectory
from zipfile import ZipFile, ZIP_DEFLATED
from check_version_sync import check

ROOT = Path(__file__).resolve().parents[1]
ROOT_FILES = {
    'AGENTS.md', 'CHANGELOG.md', 'README.md', 'RELEASE_NOTES.md',
    'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew',
    'app_identity.json', 'github-manager.json', 'VERSION',
}
SOURCE_DIRS = {'app', 'subtitle-core', 'gradle', '.github', 'design', 'docs', 'tools'}
REQUIRED = {
    'build.gradle.kts', 'settings.gradle.kts', 'app/build.gradle.kts',
    'app_identity.json', 'github-manager.json', 'VERSION',
    'gradle.properties', 'gradle/wrapper/gradle-wrapper.properties',
    'gradlew', '.github/workflows/android-apk.yml',
    'tools/package_apks.py', 'tools/validate_source.py', 'tools/package_source.py',
    'app/src/main/AndroidManifest.xml',
    'subtitle-core/build.gradle.kts',
    'subtitle-core/src/main/kotlin/br/com/uaicortes/subtitles/core/SubtitleTextCasePolicy.kt',
    'app/src/main/java/br/com/uaicortes/AppReleaseNotes.kt',
    'app/src/main/java/br/com/uaicortes/GeminiNarrationUi.kt',
}
IGNORED_DIRS = {'.git', '.gradle', '.idea', 'build', 'dist', 'dist_source',
                '__pycache__', '.pytest_cache', '.kotlin', '.cxx', 'node_modules'}
IGNORED_FILES = {'.DS_Store', 'local.properties', '.env', '.env.local'}
IGNORED_SUFFIXES = ('.p12', '.pfx', '.jks', '.keystore', '.key', '.apk', '.aab',
                    '.class', '.pyc', '.log', '.hprof')


def is_source_file(file: Path) -> bool:
    relative = file.relative_to(ROOT)
    if file.is_symlink() or not file.is_file():
        return False
    if any(part in IGNORED_DIRS for part in relative.parts):
        return False
    if any(part.startswith('.env') for part in relative.parts):
        return False
    if relative.name in IGNORED_FILES or relative.name.endswith(IGNORED_SUFFIXES):
        return False
    return relative.as_posix() in ROOT_FILES or relative.parts[0] in SOURCE_DIRS


def fail(message: str):
    raise RuntimeError(message)


def checked_files() -> list[Path]:
    files = sorted((f for f in ROOT.rglob('*') if is_source_file(f)),
                   key=lambda f: f.relative_to(ROOT).as_posix())
    names = {f.relative_to(ROOT).as_posix() for f in files}
    missing = sorted(REQUIRED - names)
    if missing:
        fail(f'Faltam arquivos indispensáveis no pacote: {missing}')
    return files


def build_zip(output: Path, version: str):
    files = checked_files()
    if output.resolve().is_relative_to(ROOT):
        fail('O ZIP de saída deve ficar fora do diretório de código-fonte.')
    output.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(output, 'w', ZIP_DEFLATED, compresslevel=8) as archive:
        for file in files:
            archive.write(file, file.relative_to(ROOT).as_posix())
    with ZipFile(output) as archive:
        names = archive.namelist()
        missing = sorted(REQUIRED - set(names))
        if missing:
            fail(f'ZIP incompleto: {missing}')
        if archive.testzip() is not None:
            fail('CRC inválido no ZIP')
        with TemporaryDirectory(prefix='uaicortes-source-check-') as temp:
            # Todos os nomes são relativos e gerados pelo próprio projeto.
            archive.extractall(temp)
            reconstructed = Path(temp)
            for name in REQUIRED:
                if not (reconstructed/name).is_file():
                    fail(f'Arquivo não extraído: {name}')
            validation = subprocess.run(
                [sys.executable, str(reconstructed/'tools/validate_source.py')],
                capture_output=True, text=True, cwd=temp)
            if validation.returncode:
                print(validation.stdout, file=sys.stderr)
                print(validation.stderr, file=sys.stderr)
                fail('ZIP extraído reprovou tools/validate_source.py')
    print(f'OK: fonte v{version}: {len(files)} arquivos, Gradle completo, CRC e validação no ZIP extraído')
    print(f'Arquivo: {output} ({output.stat().st_size} bytes)')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    # Falhar antes de empacotar se Gradle, workflow e metadados divergem.
    version, _code = check(ROOT)
    build_zip(args.output, version)


if __name__ == '__main__':
    main()
