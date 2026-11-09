#!/usr/bin/env python3
"""Contratos de segurança e fluxo Meta 250. Não prova acesso real à Graph API."""
from pathlib import Path
from check_version_sync import check
r=Path(__file__).resolve().parents[1]
s=r/'app/src/main/java/br/com/uaicortes'
ui=(s/'SocialFacebookPageLibraryUi.kt').read_text()
api=(s/'SocialFacebookPagePosts.kt').read_text()
auth=(s/'SocialMetaAuth.kt').read_text()
dash=(s/'SocialScreens.kt').read_text()
store=(s/'SocialStore.kt').read_text()
version, version_code = check(r)
# A funcionalidade introduzida na 250 deve persistir nas versões posteriores.
assert tuple(int(part) for part in version.split('.')) >= (0, 1, 250)
assert version_code >= 251
assert 'activePlatform == "youtube"' in dash and 'activePlatform == "facebook"' in dash
assert 'SocialFacebookPageLibraryUi(' in dash and 'SocialYouTubeLibraryUi(' in dash
assert 'fun fetchFacebookPosts(' in dash and 'onLoad = ::fetchFacebookPosts' in dash
assert 'SocialExternalApis.facebookPages(userToken)' in dash and 'SocialMetaAuth.pageReadToken(id)' in dash
assert 'pages.firstOrNull { it.id == id }' in dash
assert 'LaunchedEffect(facebookBusy, pendingFacebookPages)' in dash
assert 'Log.' not in api and 'access_token=' not in api and 'pageToken' not in store
assert 'Authorization", "Bearer $token"' in api
assert 'graph.facebook.com/v26.0' in api and '/posts?limit=25' in api
assert 'safeCursor(cursor)' in api and 'limit=25' in api
assert 'attachments{media_type,type,media}' in api
assert 'type.startsWith("video_")' in api and 'mediaType == "video"' in api
assert 'host.endsWith(".fbcdn.net")' in api and 'uri.scheme == "https"' in api
assert 'setOf("facebook.com", "www.facebook.com", "m.facebook.com")' in api
assert 'pageReadToken' in auth and 'pages_read_user_content' in auth
assert 'pages_manage_posts' in auth and 'fun pageToken(' in auth
assert 'visualizações não disponíveis' in ui and 'Visualizações: —' in ui
assert 'chunked(2)' in ui and 'SocialFacebookThumbnail' in ui
assert 'if (videos.isEmpty() && !busy)' in ui and 'A Meta pode omitir Reels' in ui
assert 'currentUserToken()' in dash and 'connectFacebookReader()' in dash
assert 'SocialPublicationStatus.CONFIRMED_BY_API' in (s/'SocialYouTubeLibraryUi.kt').read_text()
print('OK: filtro Meta, verificação de Página, consulta manual, cursor, URLs e métricas reais')
