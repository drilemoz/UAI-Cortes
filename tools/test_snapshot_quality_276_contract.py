#!/usr/bin/env python3
"""Contrato v0.1.276: print do editor com mais qualidade. Checagem estrutural, não é build Android."""
from pathlib import Path
src = Path(__file__).resolve().parents[1] / 'app/src/main/java/br/com/uaicortes'
rd = lambda n: (src / n).read_text(encoding='utf-8')
assert 'fun snapshotTier' in rd('FrameSnapshotTiming.kt')
assert 'FrameSnapshotTiming.snapshotTier(sourceShortSide)' in rd('FrameSnapshotProject.kt')
# o CI compila FrameSnapshotProject isoladamente: nada fora de FrameSnapshotTiming/FrameCapturePlan
assert 'VideoExportQuality' not in rd('FrameSnapshotProject.kt')
assert 'ExportBitratePreset.MAXIMUM' in rd('FrameSnapshotRenderer.kt')
assert 'SNAPSHOT_KEYFRAME_INTERVAL_SECONDS' in rd('ExportEngine.kt')
saver = rd('VideoFrameSaver.kt')
assert 'Bitmap.Config.ARGB_8888' in saver and saver.count('frameAt(retriever,') == 2
print('OK: v0.1.276 print com resolução válida, bitrate máximo e ARGB_8888')
