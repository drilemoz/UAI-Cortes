#!/usr/bin/env python3
"""Regressão do vínculo de conta por canal, separação de escopos e confidencialidade.
Não substitui teste de OAuth real/compilação Android.
"""
from pathlib import Path
r=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/uaicortes'
a=(r/'YouTubeAuthorizationUi.kt').read_text()
s=(r/'SocialScreens.kt').read_text()
store=(r/'SocialStore.kt').read_text()
publish=(r/'SocialPublishScreen.kt').read_text()
email=(r/'YouTubeGoogleAccountEmail.kt').read_text()
assert 'builder.setAccount(Account(accountEmail, "com.google"))' in a
assert 'if (!accountEmail.isNullOrBlank())' in a
assert 'else if (chooseAccount)' in a
assert 'AuthorizationRequest.Prompt.SELECT_ACCOUNT' in a
assert 'YouTubePermission.PUBLISH' in a
assert 'YouTubePermission.READ' in a
assert 'https://www.googleapis.com/auth/youtube.readonly' in a
assert 'https://www.googleapis.com/auth/youtube.upload' in a
assert 'forChannel(accountEmail: String' in a
assert 'forUpload(accountEmail: String?' in a
assert 'YouTubePermission.PUBLISH ->' in a
assert 'getAuthorizationResultFromIntent' in a
assert 'requestOfflineAccess' not in a
assert 'googleAccountEmail' in store and 'schema") == 1' in store
assert 'YouTubeGoogleAccountEmail.valid(it.googleAccountEmail)' in store
assert 'if (account.remoteChannelId == id) account.googleAccountEmail else ""' in store
assert '.put("accessToken"' not in store and 'getString("accessToken"' not in store
assert 'googleLinkTarget' in s and 'googleLinkInput' in s
assert 'YouTubeSocialApi.channel(token)' in s
assert 'require(actual.id == target.remoteChannelId)' in s
assert 'it.copy(googleAccountEmail = normalized)' in s
assert 'autorizeYouTube(true)' not in s
assert 'authorizeYouTube.forChannel(email)' in s
assert 'Associe uma conta Google' in s
assert 'authorizeYouTube.forUpload(account.googleAccountEmail.ifBlank { null })' in publish
assert 'require(channel.id == account.remoteChannelId)' in publish
assert 'emailPattern' in email and 'fun normalized(raw: String)' in email
assert 'GoogleSignInAccount' not in a
assert 'SharedPreferences' not in a
print('OK: autorização YouTube por conta e canal, escopos e proteção de token')
