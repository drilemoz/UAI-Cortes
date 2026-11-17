#!/usr/bin/env python3
"""Contrato v0.1.277: tela Exportar simplificada. Checagem estrutural, não é build Android."""
from pathlib import Path
s = (Path(__file__).resolve().parents[1] / 'app/src/main/java/br/com/uaicortes/ExportScreen.kt').read_text(encoding='utf-8')
assert 'listOf(720,1080).forEach' in s and 'listOf(360,480,720,1080)' not in s.replace('listOf(360, 480, 720, 1080)', '')
assert 'Detalhes e compatibilidade' not in s and 'Ocultar detalhes técnicos' not in s and 'qualityHelpExpanded' not in s
assert '"Vertical 9:16"' in s and '"Shorts"' not in s and '"Reels"' not in s and '"TikTok"' not in s
assert 'if(destination.isVertical) {\n            Surface(onClick={previewExpanded' in s
assert s.index('CompactExportLabel("Resolução"') < s.index('CompactExportLabel("Formato"') < s.index('CompactExportLabel("Áudio e legendas na exportação"')
print('OK: v0.1.277 Exportar só 720p/1080p, Formato Original/Vertical, sem detalhes técnicos')
