#!/usr/bin/env python3
"""Reproduz o erro de visibilidade Kotlin do CI 70 com compilação JVM isolada.

Não compila Android/Media3. O arquivo de produção é coberto também por
validate_source.py; este teste prova que o compilador rejeita a assinatura pública.
"""
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
source = (root / 'app/src/main/java/br/com/uaicortes/ExportEngine.kt').read_text()
assert re.search(r'\binternal\s+fun\s+start\s*\(\s*project:\s*VideoProject', source)
assert 'preset: ExportBitratePreset = ExportBitratePreset.HIGH' in source
assert 'onSuccess: (Uri, ExportQualityReport) -> Unit' in source
assert 'internal enum class ExportBitratePreset' in (root / 'app/src/main/java/br/com/uaicortes/VideoExportQuality.kt').read_text()
assert 'internal data class ExportQualityReport' in (root / 'app/src/main/java/br/com/uaicortes/ExportQualityReport.kt').read_text()

kotlinc = shutil.which('kotlinc')
if not kotlinc:
    print('SKIP: kotlinc não disponível; a verificação estática foi aplicada')
    raise SystemExit(0)

fixture = '''package br.com.uaicortes
internal enum class ExportBitratePreset { HIGH }
internal data class ExportQualityReport(val code: Int)
class ExportEngine {
    FUN start(preset: ExportBitratePreset = ExportBitratePreset.HIGH,
              onSuccess: (String, ExportQualityReport) -> Unit) {
        onSuccess("test", ExportQualityReport(1))
    }
}
'''
with tempfile.TemporaryDirectory() as directory:
    temp = Path(directory)
    def compile_case(declaration: str):
        path = temp / 'Contract.kt'
        path.write_text(fixture.replace('FUN', declaration), encoding='utf-8')
        return subprocess.run([kotlinc, str(path), '-d', str(temp / 'test.jar')],
                              capture_output=True, text=True, timeout=40)
    bad = compile_case('fun')
    assert bad.returncode != 0 and 'exposes its' in (bad.stderr + bad.stdout), (
        f'Controle negativo não reproduziu erro: {bad.returncode}, {bad.stderr}')
    good = compile_case('internal fun')
    assert good.returncode == 0, f'Assinatura internal reprovada: {good.stderr}'
    print('OK: compilador Kotlin rejeita assinatura public e aprova assinatura internal')
