#!/usr/bin/env python3
"""Regressão v221: painéis de voz por grupo, sem perda de funções v220.

Inspeção estrutural: a compilação e a navegação Android continuam necessárias.
"""
from pathlib import Path
r = Path(__file__).resolve().parents[1]
s = r/'app/src/main/java/br/com/uaicortes'
panel = (s/'VoiceTonePanel.kt').read_text()
for label in ('Melhorar voz', 'Estilo', 'Ajustes', 'Equalizador', 'Limpeza',
              'Meus ajustes', 'Original', 'Tratada', 'Compressão', 'Compensar volume',
              'Intensidade', 'Graves', 'Corpo / médios', 'Clareza', 'Brilho',
              'Redução de ruído leve', 'Suavizar S e CH', 'Zerar EQ manual',
              'Salvar ajuste', 'Usar ajuste', 'Perfil $slot', 'Copiar para vídeos'):
    assert label in panel, label
assert panel.count('VoiceSettingsPage.') >= 15
assert 'ModalBottomSheet(' in panel
assert 'shape = UaiShapes.sheet' in panel
assert 'shape = UaiShapes.button' in panel
assert 'shape = UaiShapes.dialog' in panel
assert '.verticalScroll(rememberScrollState())' in panel
assert '.horizontalScroll(' not in panel
assert 'overflow = TextOverflow.Ellipsis' in panel
assert 'Modifier.weight(1f)' in panel
assert 'selectedId = source.id' in panel
assert 'if (it.isImage) it else it.copy(voiceTone = tone)' in panel
assert 'project.music' not in panel
assert 'TextButton(onClick = { activePage = VoiceSettingsPage.PROFILES })' in panel  # Restaurar perfil com efeito desligado
assert 'favorites.save(selectedSlot, tone)' in panel
assert 'favorites.load(selectedSlot)' in panel
assert 'change(loaded)' in panel
assert 'onPreview(selected.id, false)' in panel and 'onPreview(selected.id, true)' in panel
assert 'VoiceTone.OFF' in panel and 'VoiceTone.STUDIO' in panel
assert 'schemaVersion' not in panel  # Nenhuma migração de dados para alteração visual
assert '.put("schemaVersion", 4)' in (s/'ProjectStore.kt').read_text()
assert 'VoiceToneProcessor' in (s/'ExportEngine.kt').read_text()
print('OK: v221 layout compacto, pop-ups, presets, EQ, perfis, A/B e áudio preservados')
