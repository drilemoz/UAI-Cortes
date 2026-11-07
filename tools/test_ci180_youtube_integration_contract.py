#!/usr/bin/env python3
"""CI180: upload e teste YouTube sobrevivem à separação da Central v217.

Contrato estrutural; executar build e OAuth reais em Android no CI/aparelho.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
dashboard = (src / 'SocialScreens.kt').read_text(encoding='utf-8')
center = (src / 'SocialIntegrationsUi.kt').read_text(encoding='utf-8')
auth = (src / 'YouTubeAuthorizationUi.kt').read_text(encoding='utf-8')

# Publicar é uma ação explícita: somente canal vinculado e sem envio concorrente.
assert 'Text("Enviar ao YouTube pela API")' in dashboard
assert 'enabled = !youtubeBusy && account.remoteChannelId.isNotBlank()' in dashboard
assert 'uploadToConfirm = publication' in dashboard
assert 'YouTubeSocialApi.upload(context, token, marked,' in dashboard
assert 'require(channel.id == account.remoteChannelId)' in dashboard
assert 'SocialPublicationPolicy.markUploadReview(source)' in dashboard
assert 'SocialPublicationPolicy.confirmApiUpload(' in dashboard

# A Central testa a conta com autorização Google real, sem persistir token OAuth.
assert center.count('internal fun SocialApiCenterScreen(') == 1
assert 'val youtubeAuth = rememberYouTubeAuthorization' in center
assert 'youtubeAuth(true) { token ->' in center
assert 'YouTubeSocialApi.channel(token)' in center
assert 'enabled = !busy' in center
assert 'Text("Conectar canal YouTube")' in center
assert 'Identity.getAuthorizationClient' in auth
assert 'putString("token"' not in auth

print('OK: CI180 - envio YouTube protegido e teste OAuth mantidos após modularização da Central')
