#!/usr/bin/env python3
"""Sobre compacto sem desabilitar novidades automáticas após atualização."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
sources=r/'app/src/main/java/br/com/uaicortes'
home=(sources/'HomeScreens.kt').read_text()
notes=(sources/'AppReleaseNotes.kt').read_text()
main=(sources/'MainActivity.kt').read_text()
news=(sources/'WhatsNewScreen.kt').read_text()
assert 'fun forAbout(): List<ReleaseSummary> = listOf(current.copy(features = current.features.take(1)))' in notes
assert 'Adriedson Lemos' in home and 'drilemoz@gmail.com' in home
assert 'Copiar chave Pix' in home and 'context.startActivity(intent)' in home
assert 'BuildConfig.GITHUB_RELEASE_REPOSITORY' in home and 'github.com/$repository' in home
# A partir de 0.1.248 o item duplicado de Novidades saiu de Configurações.
assert '"Novidades","Mudanças' not in home
assert '"Geral","Qualidade e armazenamento"' in home
assert 'showOlderReleases' not in home
assert 'whatsNewSeen.shouldOpen()' in main and 'whatsNewSeen.markViewed()' in main
assert 'ReleaseHighlights(AppReleaseNotes.forWhatsNew())' in news
assert 'ReleaseHighlights(AppReleaseNotes.forHistory())' not in news
assert 'fun forHistory(): List<ReleaseSummary>' in notes
assert 'Button(onClick = onContinue' in news
print('OK: Sobre compacto, Novidades apenas após atualização, histórico retido em dados e sem entrada duplicada em Ajustes')
