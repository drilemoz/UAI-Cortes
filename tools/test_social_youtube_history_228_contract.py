#!/usr/bin/env python3
"""Regressão v0.1.228: biblioteca dos vídeos antigos separada dos envios locais.

Este teste verifica contratos estruturais e segurança; não substitui Google OAuth real.
"""
from pathlib import Path
from check_version_sync import check
root = Path(__file__).resolve().parents[1]
base = root/'app/src/main/java/br/com/uaicortes'
api = (base/'YouTubeSocialApi.kt').read_text(encoding='utf-8')
ui = (base/'SocialYouTubeLibraryUi.kt').read_text(encoding='utf-8')
dash = (base/'SocialScreens.kt').read_text(encoding='utf-8')
history = (base/'SocialPublicationHistoryUi.kt').read_text(encoding='utf-8')
auth = (base/'YouTubeAuthorizationUi.kt').read_text(encoding='utf-8')
store = (base/'SocialStore.kt').read_text(encoding='utf-8')
version, code = check(root)
assert int(version.rsplit('.', 1)[-1]) >= 228 and code >= 229
assert 'suspend fun previousVideos(' in api
assert 'part=id%2CcontentDetails&mine=true' in api
assert 'playlistItems?part=snippet%2CcontentDetails' in api
assert 'relatedPlaylists' in api and 'maxResults=20' in api
assert 'nextPageToken' in api and 'pageToken' in api
assert 'URLEncoder.encode(uploadsId' in api and 'URLEncoder.encode(pageToken' in api
assert 'current.optString("id") != expectedChannelId' in api
assert 'validVideoId(id)' in api and 'require(SocialPublicationPolicy.validChannel(expectedChannelId)' in api
assert 'ensureOk(playlistConnection, "Vídeos anteriores do YouTube")' in api
assert 'finally { playlistConnection.disconnect() }' in api
assert 'partial' not in api[api.index('suspend fun previousVideos('):api.index('suspend fun metrics(')]
assert 'SocialYouTubeLibraryUi(' in dash and 'onLoad = ::fetchOldYouTubeVideos' in dash
assert 'youtubeLibraryVideos' in dash and 'youtubeLibraryNextPage' in dash
assert 'YouTubeSocialApi.previousVideos(token, account.remoteChannelId' in dash
assert 'authorizeYouTube(true)' in dash
assert '.distinctBy { it.id }.take(300)' in dash
assert 'LaunchedEffect' not in ui and 'SocialStore(' not in ui
# Desde a v0.1.235, os carregamentos são separados por canal, com lista agregada.
# A verificação antiga exigia rótulos/estado descartados, impedindo o CI antes do Gradle.
assert 'onAccountSelected()' in ui
assert 'Filtrar títulos carregados' in ui
if int(version.rsplit('.', 1)[-1]) >= 235:
    assert 'Carregar mais do canal selecionado' in ui
    assert 'Buscar vídeos do canal selecionado' in ui
    assert 'nextPageTokensByChannel[selected?.remoteChannelId]' in ui
    assert 'videosByChannel[selected?.remoteChannelId]' in ui
    assert 'SocialYouTubeLibraryAggregate.merge(videosByChannel)' in ui
    assert 'socialFormatCount(totalViews)' in ui and 'video.viewCount' in ui
    assert 'youtubeLibraryVideosByChannel' in dash and 'youtubeLibraryNextPages' in dash
    assert 'val previous = if (isNextPage)' in dash
    assert '(account.remoteChannelId to merged)' in dash
    assert '(account.remoteChannelId to if (merged.size >= 300)' in dash
    assert 'enrichPreviousVideos(token, videos)' in api
    assert 'videos?part=snippet%2Cstatistics&id=' in api
    assert 'viewCount' in api
else:
    assert 'Carregar mais vídeos antigos' in ui and 'Buscar vídeos anteriores' in ui
    assert 'loadedChannelId == selected?.remoteChannelId' in ui
assert 'https://www.youtube.com/watch?v=${video.id}' in ui
assert 'Envios pelo Uai Cortes' in history
assert 'horizontalScroll(rememberScrollState())' in history
assert 'youtube.readonly' in auth and 'requestOfflineAccess' not in auth
assert 'accessToken' not in store and 'refreshToken' not in store
assert 'SocialPublicationHistoryUi(' in dash
print('OK: biblioteca de vídeos YouTube, OAuth por canal, paginação, métricas e histórico separado.')
