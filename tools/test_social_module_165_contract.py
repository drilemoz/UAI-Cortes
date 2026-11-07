#!/usr/bin/env python3
"""Verificações estruturais da primeira fase de Redes Sociais.

Não substituem o build Kotlin nem autorização OAuth, teste Android SAF ou publicação.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = root/'app/src/main/java/br/com/uaicortes'
store = (source/'SocialStore.kt').read_text(encoding='utf-8')
screens = '\n'.join((source/name).read_text(encoding='utf-8') for name in ('SocialScreens.kt', 'SocialAccountDialogs.kt', 'SocialPublishScreen.kt', 'SocialPublicConfig.kt',))
integrations = (source/'SocialIntegrationsUi.kt').read_text(encoding='utf-8')
nav = (source/'EditorNavigation.kt').read_text(encoding='utf-8')
main = (source/'MainActivity.kt').read_text(encoding='utf-8')
home = (source/'HomeScreens.kt').read_text(encoding='utf-8')
saved = (source/'VideoSavedDialog.kt').read_text(encoding='utf-8')
animated = (source/'AnimateImagesScreen.kt').read_text(encoding='utf-8')
export = (source/'ExportScreen.kt').read_text(encoding='utf-8')
assert 'FACEBOOK_PAGE(' in store and 'FACEBOOK_PROFILE(' in store
assert 'YOUTUBE(' in store and 'TIKTOK(' in store and 'KWAI(' in store
assert 'AtomicFile(' in store and 'load() // Nunca substituir' in store
assert 'schema") == 1' in store or 'getInt("schema") == 1' in store
assert 'videoUri == videoUri' in store and 'SocialPublicationStatus.PREPARED' in store
assert 'SocialPublicationStatus.SHARED_UNCONFIRMED' in screens
assert 'CONFIRMED_MANUALLY' in screens
assert 'Métricas informadas manualmente' in screens
assert 'socialCaption(' in screens and 'Intent.ACTION_SEND' in screens
assert 'FLAG_GRANT_READ_URI_PERMISSION' in screens and 'ClipData.newUri' in screens
assert 'takePersistableUriPermission' in screens and 'ActivityResultContracts.OpenDocument()' in screens
assert 'ActivityResultContracts.CreateDocument("application/json")' in integrations
assert 'uai-social-public-v1' in screens and 'require(json.length() == 2' in screens
assert 'require(entry.length() == 2)' in screens and 'put("profileUrl", account.profileUrl)' not in screens
assert 'Conectar canal YouTube' in integrations
assert 'SocialApiCenterScreen(' in main and 'SocialDashboardScreen(' in main
assert '"social_publish"' in nav and '"social"' in nav and '"api_center"' in nav
assert 'MainNav("social", onTab)' in screens and 'Triple("social",Icons.Default.Public,"Redes")' in home
assert 'onSocialPublish' in saved and 'Publicar vídeo' in saved
assert 'onSocialPublish' in export and 'onSocialPublish' in animated
assert f'versionCode = {__import__("check_version_sync").check(root)[1]}' in (root/'app/build.gradle.kts').read_text()
assert '.put("schemaVersion", 4)' in (source/'ProjectStore.kt').read_text()
assert 'class SocialStoreAndroidTest' in (root/'app/src/androidTest/java/br/com/uaicortes/SocialStoreAndroidTest.kt').read_text()
print('OK: Redes Sociais fase 1: rotas, segurança local, exportação, compartilhamento e APIs honestas.')
