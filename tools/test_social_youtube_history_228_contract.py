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
assert 'playlistItems?part=snippet%2CcontentDetails' in api
assert 'relatedPlaylists' in api and 'maxResults=20' in api
assert 'nextPageToken' in api and 'pageToken' in api
assert 'URLEncoder.encode(uploadsId' in api and 'URLEncoder.encode(pageToken' in api
assert 'current.optString("id") != expectedChannelId' in api
assert 'validVideoId(id)' in api and 'require(SocialPublicationPolicy.validChannel(expectedChannelId)' in api
assert 'ensureOk(playlistConnection, "Vídeos anteriores do YouTube")' in api
assert 'finally { playlistConnection.disconnect() }' in api
assert 'SocialYouTubeLibraryUi(' in dash and 'onLoad = ::fetchOldYouTubeVideos' in dash
assert 'youtubeLibraryVideosByChannel' in dash and 'youtubeLibraryNextPages' in dash
assert '.distinctBy { it.id }.take(300)' in dash
assert 'LaunchedEffect' not in ui and 'SocialStore(' not in ui
if int(version.rsplit('.', 1)[-1]) >= 238:
    assert 'part=id%2CcontentDetails%2Cstatistics&mine=true' in api
    if int(version.rsplit('.', 1)[-1]) >= 254:
        assert 'authorizeYouTube.forChannel(email)' in dash
        assert 'withVerifiedToken(token)' in dash
        assert 'youtubeLibraryError = "Associe uma conta Google' in dash
    else:
        assert 'authorizeYouTube(false)' in dash
        assert 'withVerifiedToken(token, true)' in dash
    assert 'channelStatistics = youtubeChannelStats' in dash
    assert 'videosByChannel = youtubeLibraryVideosByChannel' in dash
    assert 'mergeWithLocal(videosByChannel, publications, accounts)' in ui
    assert 'nextPageTokensByChannel[it.remoteChannelId]' in ui
    assert 'enrichPreviousVideos(token, videos)' in api
    assert 'viewCount' in api and 'SocialMetricTile("Visualizações"' in ui
else:
    assert 'part=id%2CcontentDetails&mine=true' in api
    assert 'authorizeYouTube(true)' in dash
    assert 'youtubeLibraryVideos' in dash
    assert 'Filtrar títulos carregados' in ui
    assert 'SocialYouTubeLibraryAggregate.merge(videosByChannel)' in ui
assert 'https://www.youtube.com/watch?v=$id' in ui
assert 'Envios pelo Uai Cortes' in history
assert 'horizontalScroll(rememberScrollState())' in history
assert 'youtube.readonly' in auth and 'requestOfflineAccess' not in auth
assert 'accessToken' not in store and 'refreshToken' not in store
assert 'SocialPublicationHistoryUi(' in dash
print('OK: biblioteca de vídeos YouTube, OAuth por canal, paginação, métricas e histórico separado.')
