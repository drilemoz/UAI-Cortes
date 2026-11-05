#!/usr/bin/env python3
"""Verificações estáticas da fase Meta/TikTok; não equivalem a testes com API aprovada."""
from pathlib import Path
r = Path(__file__).resolve().parents[1]
src = r/'app/src/main/java/br/com/uaicortes'
api = (src/'SocialExternalApis.kt').read_text()
ui = (src/'SocialExternalDialogs.kt').read_text()
main = (src/'SocialScreens.kt').read_text()
store = (src/'SocialStore.kt').read_text()
assert 'v26.0' in api and 'facebookPage(token)' not in api
assert 'https://open.tiktokapis.com' in api
assert '/v2/post/publish/creator_info/query/' in api
assert '/v2/post/publish/video/init/' in api
assert '/v2/post/publish/status/fetch/' in api
assert 'creator.privacyOptions' in api and 'privacy_level' in api
assert 'is_aigc' in api and 'disable_comment' in api
assert 'Content-Range' in api and 'total_chunk_count' in api
assert 'uploadUri.host == "open-upload.tiktokapis.com"' in api
assert 'upload_phase' in api and 'start' in api and 'finish' in api
assert 'host == "rupload.facebook.com"' in api and 'offset' in api and 'file_size' in api
assert 'instanceFollowRedirects = false' in api
assert 'Bearer $token' in api and 'OAuth $token' in api
assert 'putString("token"' not in api + ui
assert 'PasswordVisualTransformation()' in ui
assert 'SocialPublicationPolicy.markUploadReview(source)' in ui
assert ui.index('store.save(started)') < ui.index('SocialExternalApis.publishFacebookReel')
assert 'SocialPublicationPolicy.markExternalSubmitted' in ui
assert 'validRemoteReference' in store and 'remotePageId' in store
assert 'REMOTE_SUBMITTED' in store
assert 'SocialExternalPublishDialog' in main and 'SocialFacebookPageBindDialog' in main
assert 'SocialExternalApiTestDialog' in main and 'SocialTikTokStatusDialog' in main
assert 'SocialDestination.FACEBOOK_PROFILE' not in api
assert 'SocialDestination.KWAI' not in api
assert (r/'app/src/androidTest/java/br/com/uaicortes/SocialExternalAndroidTest.kt').is_file()
assert '.put("schemaVersion", 4)' in (src/'ProjectStore.kt').read_text()
print('OK: Meta Página e TikTok, teste e confirmação por ação, tokens temporários e recuperação sem retry')
