#!/usr/bin/env python3
"""CI199: canais vazios e cópias de mídia indexadas na galeria."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
app = root/'app/src/main/java/br/com/uaicortes'
ui = (app/'SocialYouTubeLibraryUi.kt').read_text()
assert 'fun loadedChannelCount(' in ui
assert 'videosByChannel.count { it.value.isNotEmpty() }' in ui
media = (app/'ProjectMediaCopies.kt').read_text()
assert 'val dir = File(context.filesDir, "project_media_v1/$projectId/${kind.name.lowercase()}")' in media
assert 'FileProvider.getUriForFile' in media
assert 'partial.renameTo(privateFile)' in media
assert 'privateFile.exists()' in media
assert 'MediaStore.MediaColumns.RELATIVE_PATH' in media  # legado sem migração destrutiva
copy = media.split('suspend fun copy(context:', 1)[1]
assert 'resolver.insert(' not in copy
assert 'IS_PENDING' not in copy
assert 'resolver.update(' not in copy
assert 'media_v1' not in (app/'GalleryVideoSaver.kt').read_text()
print('OK: CI199, contagem de canais com vídeos e backup privado fora do MediaStore')
