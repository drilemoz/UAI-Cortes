#!/usr/bin/env python3
"""Media3 estável e exportação conservadora; contrato, não teste Android."""
from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
gradle = (root/'app/build.gradle.kts').read_text(encoding='utf-8')
modules = dict(re.findall(r'androidx\.media3:(media3-[\w-]+):(\d+\.\d+\.\d+)', gradle))
expected = {'media3-exoplayer', 'media3-ui', 'media3-transformer', 'media3-effect', 'media3-common'}
assert set(modules) == expected, f'Módulos Media3 inesperados: {modules}'
assert set(modules.values()) == {'1.11.1'}, f'Media3 desalinhado: {modules}'
engine = (root/'app/src/main/java/br/com/uaicortes/ExportEngine.kt').read_text(encoding='utf-8')
saver = (root/'app/src/main/java/br/com/uaicortes/GalleryVideoSaver.kt').read_text(encoding='utf-8')
report = (root/'app/src/main/java/br/com/uaicortes/ExportQualityReport.kt').read_text(encoding='utf-8')
assert 'experimentalSetMp4EditListTrimEnabled' not in engine, 'Corte com edit list pode expor quadros removidos'
assert 'experimentalSetTrimOptimizationEnabled' not in engine, 'Não habilitar sem export simples elegível'
assert 'Presentation.createForShortSide(height)' in engine
assert 'setVideoMimeType(MimeTypes.VIDEO_H264)' in engine
assert 'encodeElapsedMs = encodeElapsedMs' in engine
assert 'galleryCopyElapsedMs =' in engine
assert 'val buffer = ByteArray(COPY_BUFFER_BYTES)' in saver
assert 'input.read(buffer)' in saver and 'output.write(buffer, 0, size)' in saver
assert 'if (!shouldContinue()) throw java.util.concurrent.CancellationException(' in saver
assert 'COPY_BUFFER_BYTES = 256 * 1024' in saver
assert 'check(copied == source.length())' in saver
assert 'resolver.delete(target, null, null)' in saver
assert 'encodeSpeedFactor' in report
print('OK: cinco módulos Media3 1.11.1, trim protegido, medição real e cópia local limitada')
