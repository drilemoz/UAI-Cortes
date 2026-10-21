#!/usr/bin/env python3
"""Valida e nomeia os cinco APKs Release a partir do metadata real do AGP.

Uso (no runner do GitHub): python3 tools/package_apks.py
Não escolhe o primeiro APK de um diretório, nem publica Debug/AndroidTest.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

VERSION = '0.1.37'
VERSION_CODE = 38
APP_ID = 'br.com.uaicortes'
TARGET_SDK = '36'
ABI_NAMES = {
    'armeabi-v7a': 'arm32',
    'arm64-v8a': 'arm64',
    'x86': 'x86',
    'x86_64': 'x86_64',
    None: 'universal',
}
ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'app/build/outputs/apk/release'
DEST = ROOT / 'dist'

def run(cmd):
    proc = subprocess.run(cmd, capture_output=True, text=True, check=True)
    return proc.stdout + proc.stderr

def main():
    metadata_file = SRC / 'output-metadata.json'
    metadata = json.loads(metadata_file.read_text(encoding='utf-8'))
    outputs = metadata['elements']
    if len(outputs) != len(ABI_NAMES):
        raise RuntimeError(f'Esperados 5 APKs Release, recebidos {len(outputs)} no metadata AGP')
    sdk = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
    aapt, apksigner = sdk / 'aapt', sdk / 'apksigner'
    if DEST.exists():
        shutil.rmtree(DEST)
    DEST.mkdir()
    seen = set()
    hashes = []
    for output in outputs:
        if output.get('versionCode') != VERSION_CODE or output.get('versionName') != VERSION:
            raise RuntimeError(f'Versão divergente no APK: {output}')
        filters = output.get('filters') or []
        if not filters:
            abi = None
        elif len(filters) == 1 and filters[0].get('filterType') == 'ABI':
            abi = filters[0].get('value')
        else:
            raise RuntimeError(f'Filtros inesperados: {filters}')
        if abi not in ABI_NAMES or abi in seen:
            raise RuntimeError(f'ABI ausente/duplicada/não suportada: {abi}')
        seen.add(abi)
        source_name = output['outputFile']
        if Path(source_name).name != source_name or not source_name.endswith('.apk'):
            raise RuntimeError(f'Nome de saída inseguro: {source_name}')
        source = SRC / source_name
        if not source.is_file() or not source.stat().st_size:
            raise RuntimeError(f'Arquivo APK não encontrado: {source}')
        dest = DEST / f'UaiCortes-v{VERSION}-{ABI_NAMES[abi]}.apk'
        shutil.copyfile(source, dest)
        run([str(apksigner), 'verify', '--verbose', str(dest)])
        badging = run([str(aapt), 'dump', 'badging', str(dest)])
        pkg = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
        if not pkg or pkg.groups() != (APP_ID, str(VERSION_CODE), VERSION):
            raise RuntimeError(f'Pacote/versão não conferem: {dest.name}')
        if f"targetSdkVersion:'{TARGET_SDK}'" not in badging:
            raise RuntimeError(f'targetSdk diferente de {TARGET_SDK}: {dest.name}')
        sha = hashlib.sha256(dest.read_bytes()).hexdigest()
        hashes.append((dest.name, sha))
        print(f'APROVADO: {dest.name} | {dest.stat().st_size} bytes | {sha}')
    if seen != set(ABI_NAMES):
        raise RuntimeError(f'Conjunto de ABIs incompleto: {seen}')
    # Nunca publica uma segunda cópia do mesmo nome, um Debug ou um APK genérico.
    dist_apks = list(DEST.glob('*.apk'))
    if len(dist_apks) != 5:
        raise RuntimeError(f'Esperados exatamente 5 APKs em dist/, encontrados {len(dist_apks)}')
    (DEST/'SHA256SUMS.txt').write_text(''.join(f'{sha}  {name}\n' for name, sha in sorted(hashes)))
    print('OK: 5 APKs Release selecionados por ABI + universal, assinatura e versão verificadas')

if __name__ == '__main__':
    main()
