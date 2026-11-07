#!/usr/bin/env python3
"""Contratos estáticos da biblioteca de estilos; não substituem Gradle nem teste Android."""
from pathlib import Path
base=Path(__file__).resolve().parents[1]
src=base/'app/src/main/java/br/com/uaicortes'
model=(src/'SubtitleUserPresets.kt').read_text()
store=(src/'SubtitleUserPresetStore.kt').read_text()
ui=(src/'SubtitleUserPresetPanel.kt').read_text()
editor=(src/'SubtitleVisualEditor.kt').read_text()
assert 'SubtitleUserPresetPanel(draft,onChange)' in editor
assert 'SubtitleStyleGallery(draft,onChange)' in editor  # preserva os 12 estilos existentes
assert 'MAX_ITEMS = 20' in model and 'MAX_NAME_CHARS = 32' in model
assert 'wordTimings = emptyList(), wordColors = emptyList()' in model
assert 'position = false' in model and 'SubtitleStyles.applySelectedToAll' in model
assert all(k not in store.split('private val STYLE_KEYS')[1].split(')')[0] for k in ('"text"', '"id"', '"startMs"', '"endMs"', '"x"', '"y"'))
assert 'getSharedPreferences("subtitle_user_presets", Context.MODE_PRIVATE)' in store
assert '.commit()' in store and 'NonCancellable' in store
assert 'require(STYLE_KEYS.filterNot { it == "textCase" }.all(visual::has))' in store
assert 'if (visual.has(key)) cue.put(key, visual.get(key))' in store  # arquivos pessoais antigos ainda abrem
assert 'Não foi possível ler seus modelos' in ui
assert 'enabled = nameError == null && !busy && ready' in ui
assert 'SubtitleUserPresets.rename' in ui and 'SubtitleUserPresets.remove' in ui
assert '"Excluir modelo pessoal?"' in ui
assert 'saveAll(' in ui and 'store.write(next)' in ui
assert (base/'app/src/test/java/br/com/uaicortes/SubtitleUserPresetTest.kt').exists()
print('OK: modelos pessoais offline, sem texto/tempo/posição, nome único, persistência confirmada e edição por confirmação')
