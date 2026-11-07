#!/usr/bin/env python3
"""Regressão CI184 (Kotlin Material 3): os modais de Voz exigem opt-in.

O teste estático falha cedo; não substitui compilação real pelo Gradle.
"""
from pathlib import Path
import re
r=Path(__file__).resolve().parents[1]
p=(r/'app/src/main/java/br/com/uaicortes/VoiceTonePanel.kt').read_text(encoding='utf-8')
assert 'ModalBottomSheet(' in p and 'rememberModalBottomSheetState(skipPartiallyExpanded = true)' in p
assert re.search(r'@OptIn\(ExperimentalMaterial3Api::class\)\s*@Composable\s*internal fun VoiceTonePanel\(',p), 'Material 3 opt-in ausente no composable de voz'
assert 'import androidx.compose.material3.*' in p or 'import androidx.compose.material3.ExperimentalMaterial3Api' in p
assert 'shape = UaiShapes.sheet' in p and 'sheetHeight' in p
assert 'onPreview(selected.id, false)' in p and 'onPreview(selected.id, true)' in p
assert 'VoiceToneProcessor' in (r/'app/src/main/java/br/com/uaicortes/ExportEngine.kt').read_text(encoding='utf-8')
print('OK: CI184, API experimental Material 3 autorizada no painel de voz')
