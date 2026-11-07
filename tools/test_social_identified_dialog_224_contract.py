#!/usr/bin/env python3
"""Regressão v224: confirmação social compacta, reaproveitada, sem salvar tokens.
Checagem estrutural: a UI em aparelho e o OAuth real exigem validação Android.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/br/com/uaicortes'
dialog=(src/'SocialIdentifiedAccountDialog.kt').read_text(encoding='utf-8')
dashboard=(src/'SocialScreens.kt').read_text(encoding='utf-8')
center=(src/'SocialIntegrationsUi.kt').read_text(encoding='utf-8')
assert 'internal fun SocialIdentifiedAccountDialog(' in dialog
assert 'Dialog(onDismissRequest = onDismiss' in dialog
assert 'DialogProperties(usePlatformDefaultWidth = false)' in dialog
assert 'shape = UaiShapes.dialog' in dialog and 'shape = UaiShapes.button' in dialog
assert 'MaterialTheme.colorScheme.surface' in dialog
assert 'widthIn(max = 440.dp)' in dialog and 'verticalScroll(rememberScrollState())' in dialog
assert 'ID público: $publicId' in dialog and 'maxLines = 2' in dialog
assert 'Nenhum token será armazenado' in dialog
assert 'onConfirm: () -> Unit' in dialog and 'Button(onClick = onConfirm' in dialog
assert 'OutlinedButton(onClick = onDismiss' in dialog
assert 'SocialIdentifiedAccountDialog(' in center
assert dashboard.count('SocialIdentifiedAccountDialog(') == 4
assert 'upsertYouTube(store.load(), channel.id, channel.title)' in center
assert 'upsertYouTube(current, channel.id, channel.title)' in dashboard
assert 'upsertFacebookPage(current, page.id, page.name)' in dashboard
assert 'SocialPublicationPolicy.bindChannel(it, channel.id)' in dashboard
assert 'SocialPublicationPolicy.bindPage(it, page.id)' in dashboard
assert 'accessToken' not in dialog and 'SocialMetaAuth' not in dialog
assert 'AlertDialog(onDismissRequest = { newChannelToAdd = null }' not in dashboard
assert 'AlertDialog(onDismissRequest = { youtubeChannelToAdd = null }' not in center
print('OK: v224 confirmacao social reutilizavel, compacta, temas, permissoes e vinculos preservados.')
