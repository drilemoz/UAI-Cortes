#!/usr/bin/env python3
"""Contratos do YouTube fase 2: não substituem OAuth real, Gradle nem upload de vídeo."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
s=r/'app/src/main/java/br/com/uaicortes'
a=(s/'YouTubeSocialApi.kt').read_text()
u=(s/'YouTubeAuthorizationUi.kt').read_text()
d=(s/'SocialStore.kt').read_text()
p = '\n'.join((s/name).read_text(encoding='utf-8') for name in ('SocialScreens.kt', 'SocialPublishScreen.kt', 'SocialPublicConfig.kt',))
assert 'play-services-auth:22.0.0' in (r/'app/build.gradle.kts').read_text()
assert 'Identity.getAuthorizationClient' in u and 'AuthorizationRequest.builder()' in u
assert 'StartIntentSenderForResult' in u and 'youtube.upload' in u and 'youtube.readonly' in u
assert 'accessToken' in u and 'putString("token"' not in u and 'requestOfflineAccess' not in u
assert '/channels?part=id%2Csnippet&mine=true' in a
assert '/videos?part=statistics&id=' in a
assert 'uploadType=resumable&part=snippet,status' in a
assert 'val address = Uri.parse(session)' in a and 'address.host == "www.googleapis.com"' in a
assert 'Content-Range' in a and 'code == 308' in a and 'CHUNK = 1_048_576' in a
assert 'X-Upload-Content-Length' in a and 'setFixedLengthStreamingMode' in a
assert 'private' in a and 'unlisted' in a and 'public' in a
assert 'SocialPublicationStatus.UPLOAD_REVIEW' in d and 'SocialPublicationStatus.CONFIRMED_BY_API' in d
assert 'markUploadReview' in d and 'confirmApiUpload' in d and 'remoteVideoId' in d
assert 'remoteChannelId' in d and 'source = "youtube_api"' in a
assert 'fun validChannel' in d and 'fun validVideoId' in d
assert 'YouTubeSocialApi.channel(token)' in p and 'YouTubeSocialApi.upload(context, token' in p
assert 'YouTubeSocialApi.metrics(token, publication.remoteVideoId)' in p
assert 'Autorizar e enviar' in p
# v224: confirmação/vínculo mudaram para um diálogo reutilizável.
assert 'SocialIdentifiedAccountDialog(' in p and 'linkingExisting = true' in p
assert 'SocialPublicationPolicy.bindChannel(it, channel.id)' in p
assert 'Text("Enviar ao YouTube pela API")' in p and 'youtubeBusy' in p
# O teste OAuth foi movido com a Central, sem alterar o upload na tela Redes.
c = (s/'SocialIntegrationsUi.kt').read_text()
assert 'youtubeAuth(true) { token ->' in c and 'YouTubeSocialApi.channel(token)' in c
assert 'Text("Conectar canal YouTube")' in c
assert 'SocialPublicConfig' in p and 'remoteChannelId' not in p.split('internal object SocialPublicConfig {')[1].split('@Composable')[0]
assert (r/'app/src/androidTest/java/br/com/uaicortes/YouTubeSocialAndroidTest.kt').exists()
assert '.put("schemaVersion", 4)' in (s/'ProjectStore.kt').read_text()
print('OK: autorização explícita, canal verificado, YouTube Data API, upload sem retry automático e métricas oficiais')
