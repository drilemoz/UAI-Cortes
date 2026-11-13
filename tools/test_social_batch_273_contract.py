#!/usr/bin/env python3
"""Contratos do fluxo unificado; não substituem OAuth real e build Android."""
from pathlib import Path
from check_version_sync import check
r=Path(__file__).resolve().parents[1]
code=r/'app/src/main/java/br/com/uaicortes'
ui=(code/'SocialPublishScreen.kt').read_text()
flow=(code/'SocialUploadBatch.kt').read_text()
store=(code/'SocialStore.kt').read_text()
a=(code/'YouTubeSocialApi.kt').read_text()
version, version_code = check(r)
assert int(version.rsplit('.', 1)[-1]) >= 273 and version_code >= 274
assert 'page = "confirm"' in ui and 'page = "uploading"' in ui and 'page = "result"' in ui
assert ui.count('Confirmar e iniciar envios') == 1
assert 'Envios e compartilhamentos' not in ui
assert 'Enviar ao YouTube pela API' not in ui
assert 'for (post in batchPosts)' in ui
assert 'authorizeYouTube.forUpload(account.googleAccountEmail.ifBlank { null })' in ui
assert 'require(channel.id == account.remoteChannelId)' in ui
assert ui.index('store.save(prepared)') < ui.index('YouTubeSocialApi.upload(context, token, review,')
assert 'SocialPublicationPolicy.confirmApiUpload(actual, remoteId)' in ui
assert 'scope.launch' in ui and 'UploadRateEstimator' in ui
assert 'current.phase != UploadPhase.UPLOADING' in ui
assert 'UploadReceiptFormat.eta' in ui and 'UploadReceiptFormat.bytes' in ui
assert 'Ver último relatório de envios' in ui and 'SocialUploadReceiptStore' in flow
assert 'android.util.AtomicFile' in flow
assert 'accessToken' not in flow and 'refreshToken' not in flow and 'uploadUrl' not in flow
assert (code/'SocialUploadRateEstimator.kt').exists()
assert 'UploadPhase.REVIEW' in flow and 'UploadPhase.FAILED_BEFORE_UPLOAD' in flow
assert 'return fromColumn' in a and 'size > 0L' in a
assert 'fun uploadSize(' in a and ui.index('YouTubeSocialApi.uploadSize(context,') < ui.index('SocialPublicationPolicy.markUploadReview(source)')
assert 'UPLOAD_REVIEW' in store and 'CONFIRMED_BY_API' in store
assert (r/'app/src/test/java/br/com/uaicortes/SocialUploadBatch273Test.kt').exists()
print('OK: v273 fluxo, confirmação única, progresso, relatórios, proteção anti-duplicação e versão')
