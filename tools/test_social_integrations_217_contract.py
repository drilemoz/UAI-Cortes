#!/usr/bin/env python3
"""Regressão estrutural da Central v217; não substitui Android/OAuth real."""
from pathlib import Path
from check_version_sync import check
ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT/'app/src/main/java/br/com/uaicortes'
center = (SRC/'SocialIntegrationsUi.kt').read_text()
dashboard = (SRC/'SocialScreens.kt').read_text()
nav = (SRC/'MainActivity.kt').read_text()
store = (SRC/'SocialStore.kt').read_text()
test = ROOT/'app/src/test/java/br/com/uaicortes/SocialIntegrationGuidesTest.kt'
assert check(ROOT) == ('0.1.217', 218)
assert dashboard.count('internal fun SocialApiCenterScreen(') == 0
assert center.count('internal fun SocialApiCenterScreen(') == 1
assert 'SocialApiCenterScreen(::back' in nav and '{ screen = "social" }' in nav
assert 'SocialIntegrationGuides.forDestination(dest)' in center
assert 'SocialGuideSection(guide, showDeveloper = !guide.manualOnly)' in center
assert 'Configuração para desenvolvedores' in center
for uri in ('https://console.cloud.google.com/apis/credentials',
    'https://developers.facebook.com/apps/',
    'https://developers.tiktok.com/docs/en/content-posting-api-get-started'):
    assert uri in center
assert 'SocialDestination.KWAI' in center and 'SocialDestination.FACEBOOK_PROFILE' in center
assert 'Autorização ao utilizar' in center and 'Somente compartilhamento manual' in center
assert 'Autorizar Google e testar' in center and 'YouTubeSocialApi.channel(token)' in center
assert 'SocialMetaAuth.start(activity, forPublishing)' in center
assert 'SocialExternalApis.facebookPages(userToken)' in center
assert 'SocialMetaAuth.putPageTokens(pages)' in center
assert 'pages.map { it.copy(accessToken = "") }' in center
assert 'SocialExternalApiTestDialog' in center and 'Testar token temporário' in center
assert 'if (showBackup)' in center and 'SocialPublicConfig.export' in center
assert 'SocialPublicConfig.merge' in center
assert 'ActivityResultContracts.CreateDocument("application/json")' in center
assert 'require(result.size() + n <= 100_000)' in center
assert 'SocialPublicConfig' in dashboard and 'uai-social-public-v1' in dashboard
assert 'schemaVersion' not in center  # o catálogo social não mexe no projeto
assert 'GeminiCredentialStore' not in center  # apenas abre as configurações existentes
assert 'META_APP_SECRET' not in center and 'TIKTOK_CLIENT_SECRET' not in center
assert 'connectMeta(false, account)' in dashboard and 'consultMetaPages(account)' in dashboard
assert 'if (state.accounts.any { it.destination == SocialDestination.FACEBOOK_PAGE })' in dashboard
assert 'metricsExpanded' in dashboard and 'Preparar publicação' in dashboard
assert 'SHARED_UNCONFIRMED' in dashboard and 'CONFIRMED_MANUALLY' in dashboard
assert 'AtomicFile(' in store and 'accessToken' not in store
assert test.exists()
print('OK: Central v217 separada, guias, OAuth real, fallback, backup e interface compacta')
