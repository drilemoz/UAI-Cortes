#!/usr/bin/env python3
from pathlib import Path
from check_version_sync import check
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/br/com/uaicortes'
dash=(src/'SocialScreens.kt').read_text()
ui=(src/'SocialYouTubeLibraryUi.kt').read_text()
assert int(check(root)[0].rsplit('.', 1)[-1]) >= 249 and check(root)[1] >= 250
for token in ['Publicar vídeo','Sua central de publicações','RoundedCornerShape(22.dp)',
              'if (!accountsOnly) Surface(','if (accountsOnly && manageAccounts)']:
    assert token in dash,token
for token in ['Visão geral','Vídeos publicados','SocialMetricTile(',
              'chunked(2)','aspectRatio(16f / 9f)','SocialYoutubeThumbnail(',
              'socialFormatCount','video.viewCount?.let(::socialFormatCount)',
              'SocialVideosEmptyState','Pesquisar publicações','Nenhum vídeo corresponde à pesquisa.',
              'onLoadAll(channels)','onSelectChannel(channel.remoteChannelId)']:
    assert token in ui,token
assert 'YouTubeSocialApi.previousVideos' in dash
assert 'SocialPublicationStatus.CONFIRMED_BY_API' in ui
assert 'SocialPublicationPolicy.validVideoId(id)' in ui
assert 'query = it.take(100)' in ui
assert 'onLoad(selected, "")' in ui
assert '0.1.249' in (src/'AppReleaseNotes.kt').read_text()
print('OK: redesign visual Redes 249, preservação de consultas e versão 250')
