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
assert 'onAccountSelected()' in ui and 'Carregar mais vídeos antigos' in ui
assert 'Carregar mais vídeos antigos' in ui and 'Buscar vídeos anteriores' in ui
assert 'Filtrar títulos carregados' in ui and 'loadedChannelId == selected?.remoteChannelId' in ui
assert 'https://www.youtube.com/watch?v=${video.id}' in ui
assert 'Envios pelo Uai Cortes' in history
assert 'horizontalScroll(rememberScrollState())' in history
assert 'youtube.readonly' in auth and 'requestOfflineAccess' not in auth
assert 'accessToken' not in store and 'refreshToken' not in store
assert 'SocialPublicationHistoryUi(' in dash
print('OK: v228 paginação, OAuth de canal certo, 300 vídeos por sessão, histórico isolado e UI compacta.')
