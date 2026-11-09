#!/usr/bin/env python3
"""Regressão v0.1.227: fronteiras do módulo social e interface compacta.

Valida separação sem alterar OAuth, schema ou a regra de não repetir upload
incerto. Compilação Android e navegação acessível requerem teste real.
"""
from pathlib import Path
from check_version_sync import check
root = Path(__file__).resolve().parents[1]
s = root/'app/src/main/java/br/com/uaicortes'
parts = {p: (s/p).read_text(encoding='utf-8') for p in (
    'SocialScreens.kt', 'SocialPublishScreen.kt', 'SocialAccountDialogs.kt',
    'SocialAccountCard.kt', 'SocialPublicConfig.kt', 'SocialPublicationHistoryUi.kt',
    'YouTubeAuthorizationUi.kt', 'SocialStore.kt', 'YouTubeSocialApi.kt')}
dash, publish = parts['SocialScreens.kt'], parts['SocialPublishScreen.kt']
dialogs = parts['SocialAccountDialogs.kt']
account = parts['SocialAccountCard.kt']
history = parts['SocialPublicationHistoryUi.kt']
config = parts['SocialPublicConfig.kt']
version, code = check(root)
assert int(version.rsplit('.', 1)[-1]) >= 228 and code >= 229
assert 'internal fun SocialDashboardScreen(' in dash and 'internal fun SocialPublishScreen(' not in dash
assert 'internal fun SocialPublishScreen(' in publish and 'internal fun SocialDashboardScreen(' not in publish
assert 'internal object SocialPublicConfig' in config and 'uai-social-public-v1' in config
assert all(x in dialogs for x in ('internal fun SocialAddDestinationDialog(',
    'internal fun SocialAccountDialog(', 'internal fun SocialPublicationDetailDialog('))
assert not any(x in dash for x in ('internal fun SocialAccountDialog(',
    'internal object SocialPublicConfig', 'internal fun SocialPublishScreen('))
assert 'Text("Publicar vídeo"' in dash and 'SocialAccountCard(' in dash and 'rememberSaveable(account.id)' in account
assert 'UaiShapes.card' in account and 'if (expanded)' in account
assert 'Vincular canal' in dash and ('Verificar conta' in dash or 'Conta Google' in dash)
assert 'A autorização será conferida ao publicar' in account
assert 'expandedPublicationId' in history and 'Ações e detalhes' in history
assert history.index('SocialPublicationStatus.UPLOAD_REVIEW ->') < history.index('if (expandedPublicationId == publication.id)')
assert 'SocialPublicationHistoryUi(' in dash
assert 'YouTubeSocialApi.upload(context, token, marked' in publish
assert 'SocialPublicationPolicy.markUploadReview(source)' in publish
assert 'SocialPublicationPolicy.confirmApiUpload(' in publish
assert 'require(channel.id == account.remoteChannelId)' in publish
assert 'val snapshot = store.load()' in publish
assert 'requestOfflineAccess' not in parts['YouTubeAuthorizationUi.kt']
assert 'AtomicFile(' in parts['SocialStore.kt'] and 'put("schema", 1)' in parts['SocialStore.kt']
assert 'accessToken' not in parts['SocialStore.kt']
assert '.put("schemaVersion", 4)' in (s/'ProjectStore.kt').read_text()
assert 'upload(' not in history and 'LaunchedEffect' not in history
print('OK: v227 telas separadas, cartões compactos, estados/autorizações e histórico preservados.')
