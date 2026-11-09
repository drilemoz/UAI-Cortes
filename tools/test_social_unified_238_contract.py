#!/usr/bin/env python3
"""Regressão v0.1.238: dashboard unificado sem confundir OAuth, métricas e uploads.

É um teste estrutural; não comprova o comportamento do SDK Google nem a compilação Android.
"""
from pathlib import Path
from check_version_sync import check
base = Path(__file__).resolve().parents[1]
src = base / 'app/src/main/java/br/com/uaicortes'
version, code = check(base)
assert int(version.rsplit('.', 1)[-1]) >= 238 and code >= 239
screen = (src/'SocialScreens.kt').read_text()
ui = (src/'SocialYouTubeLibraryUi.kt').read_text()
api = (src/'YouTubeSocialApi.kt').read_text()
cache = (src/'SocialYouTubeSnapshotCache.kt').read_text()
auth = (src/'YouTubeAuthorizationUi.kt').read_text()
assert 'selectedChannelId' in screen
assert 'SocialYouTubeLibraryUi(' in screen and 'channelStatistics = youtubeChannelStats' in screen
assert 'Icons.Default.HelpOutline' in screen and 'Sobre o painel Redes' in screen
assert 'Text("Desempenho"' not in screen
assert 'if (accountsOnly && manageAccounts)' in screen and 'if (showHistory)' in screen
assert 'pendingChannelQueue' in screen and 'LaunchedEffect(youtubeBusy, pendingChannelQueue)' in screen
assert 'authorizeYouTube(false)' in screen and 'authorizeYouTube(true)' in screen
assert 'youtubeSessionTokens[channelId] = token' in screen
assert 'youtubeSessionTokens.clear()' in screen
assert 'channel.id != channelId' in screen
assert 'YouTubeSocialApi.previousVideos(token, channelId' in screen
assert 'SocialYouTubeSnapshotCache.Snapshot(' in screen
assert 'FilterChip(' in ui and 'horizontalScroll(rememberScrollState())' in ui
assert 'Text("Todos")' in ui and 'Text(channel.label' in ui
assert 'mergeWithLocal(' in ui and 'SocialPublicationStatus.CONFIRMED_BY_API' in ui
assert 'SocialPublicationPolicy.validVideoId' in ui and 'distinctBy { it.id }' in ui
assert 'SocialMetricTile("Visualizações"' in ui and 'SocialMetricTile("Inscritos"' in ui
assert 'channelTotals(' in ui and 'viewCount' in ui
assert 'part=id%2CcontentDetails%2Cstatistics&mine=true' in api
assert 'current.optString("id") != expectedChannelId' in api
assert 'statistics(viewCount%2CvideoCount%2CsubscriberCount%2ChiddenSubscriberCount)' in api
assert 'SocialStore(' not in ui and 'LaunchedEffect' not in ui
assert 'accessToken' not in cache and 'refreshToken' not in cache
assert 'AtomicFile(' in cache and 'youtube_dashboard_cache.json' in cache
assert 'requestOfflineAccess' not in auth
print('OK: Redes unificadas por canal, métricas oficiais, cache privado e OAuth por canal.')
