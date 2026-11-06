#!/usr/bin/env python3
"""Contrato v0.1.186: Sobre, retorno do editor e download consentido."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
java = root / 'app/src/main/java/br/com/uaicortes'
home = (java/'HomeScreens.kt').read_text()
main = (java/'MainActivity.kt').read_text()
manager = (java/'GitHubUpdateManager.kt').read_text()
ui = (java/'GitHubUpdateUi.kt').read_text()
modal = (java/'ProjectDraftUi.kt').read_text()
diag = (java/'GitHubUpdateDiagnostics.kt').read_text()
assert '"Atualizações pelo GitHub"' not in home
assert '"Ver novidades"' not in home and '"Testar visual da Home"' not in home
assert home.count('"Verificar atualizações"') == 1
assert '"about" -> AboutScreen' in main and 'screen = "updates"' in main
assert 'BuildConfig.GITHUB_RELEASE_REPOSITORY' in manager
assert 'preferences.getString("repository"' not in manager and 'saveRepository' not in manager
assert 'releases?per_page=20' in manager and 'readTextLimited(8_000_000)' in manager
assert 'GitHubResponseTooLargeException' in diag and 'GitHubResponseTooLargeException' in manager
assert 'OutlinedTextField' not in ui and 'Salvar repositório' not in ui
assert 'Abrir Releases' not in ui and 'Abrir Release' not in ui
assert 'manager.download(release)' in ui and 'confirmDownload' in ui
assert 'manager.installDownloaded()' in ui
assert 'getPackageArchiveInfo' in manager and 'release.sha256' in manager
assert 'FileProvider.getUriForFile' in manager
for label in ('EditorExitAction("Salvar"', 'Salvar como cópia',
              'EditorExitAction("Descartar"', 'EditorExitAction("Continuar"'):
    assert label in modal
assert 'onDecision("discard")' in modal and 'onDecision("save")' in modal
print('OK: v0.1.186 Sobre, atualizador integrado, download consentido e saída clara')
