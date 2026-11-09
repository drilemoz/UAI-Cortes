#!/usr/bin/env python3
"""Regressão v245: controles reais, mesmo referencial de legendas e exportação protegida."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
s=r/'app/src/main/java/br/com/uaicortes'
manager=(s/'GitHubUpdateManager.kt').read_text()
core=(s/'GitHubUpdateCore.kt').read_text()
updates=(s/'GitHubUpdateUi.kt').read_text()
assert 'fun pauseDownload()' in manager and 'fun stopDownload()' in manager
assert 'suspend fun resumeDownload(' in manager and 'matchingContentRange(' in manager
assert 'java.io.FileOutputStream(partial, offset > 0L)' in manager
assert 'if (!pauseRequested || stopRequested) partial.delete()' in manager
assert 'if (!digest.equals(release.sha256' in manager
assert 'fun matchingContentRange(' in core and 'last < total' in core
assert all(('"'+label+'"') in updates for label in ('Pausar','Retomar','Parar','Recomeçar'))
assert 'manager.pauseDownload()' in updates and 'manager.stopDownload()' in updates
assert 'manager.restartDownload(release)' in updates
assert 'manager.resumeDownload(release)' in updates
preview=(s/'SubtitleScreen.kt').read_text();sizing=(s/'SubtitleExportSizing.kt').read_text()
policy=(s/'SubtitleFrameTypography.kt').read_text()
assert 'previewSizeSp(' in preview and 'allowedTextWidth(' in preview
assert 'fontPixelsForFrame' in sizing and 'allowedTextWidth(' in sizing
assert 'REFERENCE_WIDTH = 280f' in policy
export=(s/'ExportScreen.kt').read_text()
assert 'confirmStartExport' in export and 'ProjectNaming.exportName(name)' in export
assert 'exportVm.start(p,quality,safeName,bitratePreset,destination,frameMode)' in export
assert 'Confirmar exportação' in export and 'A extensão .mp4 será adicionada' in export
assert 'Triple("Descrição", Icons.Default.Description, "publication")' not in (s/'EditorScreen.kt').read_text()
saved=(s/'VideoSavedDialog.kt').read_text()
assert 'Text("Assistir")' in saved and 'Text("Compartilhar")' in saved
assert 'Text("Descrição")' in saved and 'Text("Gerar com IA")' in saved
rec=(s/'DiagnosticRecorder.kt').read_text()
assert 'fun update(' in rec and 'fun exportEvent(' in rec
assert 'getHistoricalProcessExitReasons' in rec and 'REASON_ANR' in rec
print('OK: v245 atualização, legendas, exportação e diagnóstico conectados')
