#!/usr/bin/env python3
from pathlib import Path
import json
p=Path(__file__).resolve().parents[1]
code=p/'app/src/main/java/br/com/uaicortes'
social=(code/'SocialYouTubeLibraryUi.kt').read_text()
dash=(code/'SocialScreens.kt').read_text()
api=(code/'YouTubeSocialApi.kt').read_text()
thumb=(code/'SocialYoutubeThumbnailPolicy.kt').read_text()
publish=(code/'SocialPublishScreen.kt').read_text()
export=(code/'ExportScreen.kt').read_text()
new=(code/'WhatsNewScreen.kt').read_text()
settings=(code/'HomeScreens.kt').read_text()
main=(code/'MainActivity.kt').read_text()
assert 'Vídeos publicados' in social and 'chunked(2)' in social
assert 'Pesquisar publicações' in social and 'query = it.take(100)' in social and 'matches(it, query)' in social
assert 'SocialYoutubeThumbnail' in social and 'visualizações' in social and 'socialFormatCount' in social
assert 'thumbnailUrl: String = ""' in api
assert 'i.ytimg.com' in thumb and 'img.youtube.com' in thumb and '6 * 1024 * 1024' in thumb
assert 'getScaledFrameAtTime' in publish and 'Remover vídeo selecionado' in publish
assert 'if (videoUri.isBlank())' in publish and 'SocialSelectedVideoThumbnail' in publish
assert 'accountsOnly: Boolean = false' in dash and 'if (activePlatform == "youtube") SocialYouTubeLibraryUi(' in dash
assert 'if (accountsOnly && manageAccounts)' in dash
assert 'Minhas contas e canais' in settings and '"social_accounts"' in main
assert '"Novidades","Mudanças' not in settings and 'Ver histórico de versões' not in new
assert 'UaiActionDialog(' in export and 'UaiActionDialog(' in publish
assert 'Confirmar exportação' not in export and 'AlertDialog(onDismissRequest = { if (!youtubeBusy) uploadToConfirm' not in publish
from check_version_sync import check
assert json.loads((p/'github-manager.json').read_text())['android']['versionCode']==check(p)[1]
print('OK: controles redes/publicação, miniaturas, pesquisa, diálogos e versão 0.1.248')
