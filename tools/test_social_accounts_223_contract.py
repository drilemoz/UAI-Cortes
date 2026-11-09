#!/usr/bin/env python3
"""V223: contratos de seleção explícita, cadastro real, sem tokens, e erro 403 sanitizado.
Não substitui compilação Android, teste OAuth em conta real nem consulta à API ativada.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/br/com/uaicortes'
auth=(src/'YouTubeAuthorizationUi.kt').read_text()
api=(src/'YouTubeSocialApi.kt').read_text()
data=(src/'SocialStore.kt').read_text()
dash = '\n'.join((src/name).read_text(encoding='utf-8') for name in ('SocialScreens.kt', 'SocialAccountDialogs.kt', 'SocialPublishScreen.kt',))
center=(src/'SocialIntegrationsUi.kt').read_text()
assert 'AuthorizationRequest.Prompt.SELECT_ACCOUNT' in auth
assert 'if (chooseAccount)' in auth and 'requestOfflineAccess' not in auth
assert 'errorDescription(code,' in api and 'readLimited(connection)' in api
for reason in ('quotaExceeded','accessNotConfigured','insufficientPermissions'):
    assert reason in api
assert 'upsertYouTube(data: SocialData' in data and 'upsertFacebookPage(data: SocialData' in data
assert 'remoteChannelId == channelId' in data and 'remotePageId == pageId' in data
assert 'accessToken' not in data and 'put("schema", 1)' in data
assert 'SocialAddDestinationDialog(' in dash and 'Conectar canal YouTube' in dash
assert 'showMetaPagePicker' in dash and 'SocialPublicationPolicy.upsertYouTube(' in dash
assert 'SocialPublicationPolicy.upsertFacebookPage(' in dash
assert ('Canal cadastrado' in dash or 'Associe uma conta Google' in dash) and 'Página cadastrada' in dash
assert 'require(channel.id == account.remoteChannelId)' in dash
assert 'SocialPublicationPolicy.markUploadReview(source)' in dash
assert 'YouTubeSocialApi.upload(context, token' in dash
assert 'youtubeAuth(true)' in center and 'upsertYouTube(store.load()' in center
assert 'upsertFacebookPage(' in center
assert 'Token' not in api.split('private fun errorDescription')[0] if 'private fun errorDescription' in api else True
assert (root/'app/src/androidTest/java/br/com/uaicortes/SocialStoreAndroidTest.kt').read_text().count('importacaoAutomaticaDoisCanaisSemDuplicarOuPerderHistorico') == 1
print('OK: v223 OAuth com seletor, contas múltiplas, destinos automáticos, diagnóstico 403 e proteção de upload.')
