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
assert home.index('"Novidades","Mudanças da versão atual e histórico"') < home.index('"Geral","Qualidade e armazenamento"')
assert 'showOlderReleases' not in home
assert 'whatsNewSeen.shouldOpen()' in main and 'whatsNewSeen.markViewed()' in main
assert 'ReleaseHighlights(AppReleaseNotes.forWhatsNew())' in news
assert 'ReleaseHighlights(AppReleaseNotes.forHistory())' in news
assert 'Button(onClick = onContinue' in news
print('OK: Sobre compacto + Pix/GitHub, Novidades no topo, histórico e abertura pós-update preservados')
