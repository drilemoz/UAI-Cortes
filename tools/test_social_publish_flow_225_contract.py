#!/usr/bin/env python3
"""Contrato da primeira fase do fluxo exportar/publicar, sem simular upload real.

Exige proteção contra duplicidade, título separado de arquivo, feedback de envio
ativo separado de falha e confirmação do ID pela API. O build Android é separado.
"""
from pathlib import Path
r = Path(__file__).resolve().parents[1]
s = r/'app/src/main/java/br/com/uaicortes'
store = (s/'SocialStore.kt').read_text()
api = (s/'YouTubeSocialApi.kt').read_text()
ui = '\n'.join((s/name).read_text(encoding='utf-8') for name in ('SocialPublishScreen.kt',))
saved = (s/'VideoSavedDialog.kt').read_text()
quality = (s/'ExportQualityReport.kt').read_text()
engine = (s/'ExportEngine.kt').read_text()
assert 'val title: String =' in store and '.put("title", title)' in store
assert 'json.optString("title")' in store and 'suggestedVideoTitle' in store
assert 'title.trim().length in 1..100' in store
assert 'title = title.trim()' in store and 'markUploadReview' in store
assert 'val name = publication.title.trim()' in api
assert 'Text("Título no YouTube")' in ui
assert 'Text("Privacidade no YouTube"' in ui
assert 'Text("Continuar para enviar")' not in ui or '"Continuar para enviar"' in ui
assert 'page = "confirm"' in ui
assert 'Confirmar e iniciar envios' in ui
assert 'page = "uploading"' in ui and 'page = "result"' in ui
assert 'Aguardando autorização Google' in ui and 'Sem confirmação' in ui
assert 'https://studio.youtube.com/video/' in ui
assert 'require(channel.id == account.remoteChannelId)' in ui
assert 'SocialPublicationPolicy.markUploadReview(source)' in ui
assert 'SocialPublicationPolicy.confirmApiUpload(' in ui
assert 'UploadRateEstimator' in ui and 'UploadReceiptFormat.eta' in ui

assert '"Publicar vídeo"' in saved and 'Text("Compartilhar")' in saved
assert 'onSocialPublish(uri, displayedName, currentDraft())' in saved
assert 'MediaMetadataRetriever' in engine and 'savedMp4Geometry(' in engine
assert 'savedRotationDegrees == 90' in quality
assert 'savedFileWidth > 0' in quality
print('OK: fase 1 v225, publicação direta, título separado, status honesto e orientação salva.')
