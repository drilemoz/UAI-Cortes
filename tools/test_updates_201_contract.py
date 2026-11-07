#!/usr/bin/env python3
"""Regras de interface v0.1.201: histórico opt-in e telemetria local de atualização."""
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
assert 'if (showHistory)' in whats and 'ReleaseHighlights(AppReleaseNotes.forHistory())' in whats
assert 'if (showOlderReleases)' in about and 'ReleaseHighlights(AppReleaseNotes.forOlderAbout())' in about
assert 'UaiShapes.button' in ui and 'UaiShapes.dialog' in ui and 'UaiShapes.card' in ui
assert 'downloadSpeedBytesPerSecond' in ui and 'readableRate' in ui and 'readableBytes' in ui
assert 'manager.download(release)' in ui and 'manager.installDownloaded()' in ui
assert 'sha256' in ui and 'getPackageArchiveInfo' in manager
assert 'withContext(Dispatchers.Main)' in manager and 'elapsed >= 500L' in manager
assert 'private const val MIB = 1024.0 * 1024.0' in logic
assert 'downloadedBytes' in manager and 'downloadTotalBytes' in manager
assert 'downloadError' in manager and 'readyFile' in manager
print('OK: novidades da versão atual, histórico sob demanda, progresso MiB/s e instalação consentida')
