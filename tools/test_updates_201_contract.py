#!/usr/bin/env python3
"""Regras legadas de atualização preservadas; Novidades sem histórico visual desde 0.1.248."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
sources = root/'app/src/main/java/br/com/uaicortes'
notes = (sources/'AppReleaseNotes.kt').read_text()
whats = (sources/'WhatsNewScreen.kt').read_text()
about = (sources/'HomeScreens.kt').read_text()
ui = (sources/'GitHubUpdateUi.kt').read_text()
manager = (sources/'GitHubUpdateManager.kt').read_text()
logic = (sources/'GitHubDownloadProgress.kt').read_text()
assert 'fun forWhatsNew(): List<ReleaseSummary> = listOf(current)' in notes
assert 'fun forHistory(): List<ReleaseSummary> = recent.drop(1)' in notes
assert 'ReleaseHighlights(AppReleaseNotes.forWhatsNew())' in whats
# O histórico permanece em dados, sem ser exibido na tela pós-atualização.
assert 'if (showHistory)' not in whats and 'ReleaseHighlights(AppReleaseNotes.forHistory())' not in whats
assert 'Button(onClick = onContinue' in whats
assert 'ReleaseHighlights(AppReleaseNotes.forAbout())' in about
assert 'showOlderReleases' not in about
assert 'fun forAbout(): List<ReleaseSummary> = listOf(current.copy(features = current.features.take(1)))' in notes
assert 'UaiShapes.button' in ui and 'UaiShapes.dialog' in ui and 'UaiShapes.card' in ui
assert 'downloadSpeedBytesPerSecond' in ui and 'readableRate' in ui and 'readableBytes' in ui
assert 'manager.download(release)' in ui and 'manager.installDownloaded()' in ui
assert 'sha256' in ui and 'getPackageArchiveInfo' in manager
assert 'withContext(Dispatchers.Main)' in manager and 'elapsed >= 500L' in manager
assert 'private const val MIB = 1024.0 * 1024.0' in logic
assert 'downloadedBytes' in manager and 'downloadTotalBytes' in manager
assert 'downloadError' in manager and 'readyFile' in manager
print('OK: novidades atuais sem histórico visual, progresso MiB/s e instalação consentida')
