#!/usr/bin/env python3
"""Contrato do histórico e do status remoto: chamadas somente manuais, sem retry."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root/'app/src/main/java/br/com/uaicortes'
ui = (src/'SocialPublicationHistoryUi.kt').read_text()
store = (src/'SocialStore.kt').read_text()
screen = '\n'.join((src/name).read_text(encoding='utf-8') for name in ('SocialScreens.kt', 'SocialPublishScreen.kt',))
api = (src/'YouTubeSocialApi.kt').read_text()
assert all(x in ui for x in ('PENDING("Pendentes")','RECEIVED("Recebidas")','REVIEW("Revisar")',
                            'SocialPublicationHistoryUi(', 'publication.title', 'publication.youtubeState.summary()',
                            'onYouTubeStatus(publication, account)', 'onYouTubeMetrics(publication, account)'))
assert 'onShare(publication)' in ui and 'SHARED_UNCONFIRMED' in ui
assert 'CONFIRMED_BY_API' in ui and 'UPLOAD_REVIEW' in ui
assert 'https://studio.youtube.com/video/' in ui
assert 'val youtubeState: SocialYouTubeState' in store
assert 'youtubeState.toJson()' in store and 'SocialYouTubeState.fromJson' in store
assert 'SocialYouTubeState.valid(it.youtubeState)' in store
assert 'fun applyYouTubeState(' in store and 'status == SocialPublicationStatus.CONFIRMED_BY_API' in store
assert 'suspend fun videoState(' in api and 'part=status%2CprocessingDetails' in api
assert 'connect(' in api and 'ensureOk(conn, "Status do vídeo YouTube")' in api
assert 'videoState(token, publication.remoteVideoId)' in screen
assert 'channel.id == account.remoteChannelId' in screen
assert 'store.save(next); data = next' in screen
assert 'SocialPublicationPolicy.applyYouTubeState(it, remote)' in screen
assert 'SocialPublicationHistoryUi(' in screen
assert 'YouTubeSocialApi.upload(context, token, review' in screen
assert 'SocialPublicationPolicy.markUploadReview(source)' in screen
assert 'SocialPublicationPolicy.confirmApiUpload(' in screen
# Sem chamadas periódicas da API ou reenvio oculto na UI do histórico.
assert 'LaunchedEffect' not in ui and 'upload(' not in ui and 'SocialStore(' not in ui
print('OK: fase 2 v226, filtros e status remoto somente sob ação, sem upload automático.')
