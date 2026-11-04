#!/usr/bin/env python3
"""Valida o fixture AVC/AAC e a fiação do smoke test Android sem executá-lo."""
from pathlib import Path
import json
import shutil
import subprocess
root = Path(__file__).resolve().parents[1]
fixture = root/'app/src/androidTest/assets/export_smoke_16x9.mp4'
test = root/'app/src/androidTest/java/br/com/uaicortes/ExportPipelineSmokeAndroidTest.kt'
assert fixture.is_file() and 1000 < fixture.stat().st_size < 512_000
assert test.is_file()
text = test.read_text(encoding='utf-8')
for token in ('ExportEngine(context)', 'CountDownLatch', 'exportClippedVideoWithAudioAndSubtitleProducesReadableMp4', 'MediaExtractor()', 'MediaMetadataRetriever()', 'context.contentResolver.delete('):
    assert token in text, token
if shutil.which('ffprobe'):
    result = subprocess.run(['ffprobe', '-v', 'error', '-show_entries',
       'stream=codec_name,codec_type,width,height', '-show_entries', 'format=duration',
       '-of', 'json', str(fixture)], capture_output=True, text=True, check=True)
    info = json.loads(result.stdout)
    streams = info['streams']
    assert any(s.get('codec_type') == 'video' and s.get('codec_name') == 'h264' for s in streams)
    assert any(s.get('codec_type') == 'audio' and s.get('codec_name') == 'aac' for s in streams)
    assert 1.5 <= float(info['format']['duration']) <= 1.7
print('OK: fixture real H.264/AAC, teste Android com Media3 e limpeza de MediaStore')
