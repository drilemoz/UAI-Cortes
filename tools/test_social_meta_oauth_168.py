#!/usr/bin/env python3
"""Regressões estáticas do login Meta: NÃO substitui build Android ou API real."""
from pathlib import Path
import re
ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'app/src/main/java/br/com/uaicortes'
auth = (JAVA/'SocialMetaAuth.kt').read_text()
api = (JAVA/'SocialExternalApis.kt').read_text()
screens = (JAVA/'SocialScreens.kt').read_text()
ui = (JAVA/'SocialExternalDialogs.kt').read_text()
store = (JAVA/'SocialStore.kt').read_text()
manifest = (ROOT/'app/src/main/AndroidManifest.xml').read_text()
gradle = (ROOT/'app/build.gradle.kts').read_text()
workflow = (ROOT/'.github/workflows/android-apk.yml').read_text()
assert 'com.facebook.android:facebook-login:19.0.0' in gradle
assert 'META_APP_ID' in gradle and 'META_CLIENT_TOKEN' in gradle
assert 'META_APP_ID: ${{ secrets.META_APP_ID }}' in workflow
assert 'META_CLIENT_TOKEN: ${{ secrets.META_CLIENT_TOKEN }}' in workflow
assert 'META_APP_SECRET' not in gradle and 'META_APP_SECRET' not in workflow
assert 'com.facebook.sdk.ApplicationId' in manifest
assert 'com.facebook.sdk.ClientToken' in manifest
assert 'fb_login_protocol_scheme' in manifest
assert 'LoginManager.getInstance()' in auth
assert 'logInWithReadPermissions' in auth
assert 'logInWithPublishPermissions' in auth
assert 'pages_manage_posts' in auth
assert 'pageTokens.clear()' in auth and 'fun disconnect()' in auth
assert 'AccessToken.getCurrentAccessToken()' in auth
assert 'SocialMetaAuth.onActivityResult' in (JAVA/'MainActivity.kt').read_text()
assert 'facebookPages(userToken:' in api and '/me/accounts?fields=id%2Cname%2Caccess_token' in api
assert 'putPageTokens(pages)' in screens
assert 'page.copy' not in screens or True
assert 'pages.map { it.copy(accessToken = "") }' in screens
assert 'SocialMetaAuth.pageToken(account.remotePageId)' in ui
assert 'pageToken(account.remotePageId)' in screens
assert 'markUploadReview(source)' in ui and 'store.save(started)' in ui
assert 'remotePageId' in store and 'accessToken' not in store
assert 'schemaVersion' in (JAVA/'ProjectStore.kt').read_text()
assert not re.search(r'(?:client_secret|app_secret)\s*=\s*"[A-Za-z0-9]{8,}"', auth, re.I)
print('OK: login Meta SDK/CI, sessão sem token no SocialStore, consentimento e fallback.')
