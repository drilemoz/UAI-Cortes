#!/usr/bin/env python3
"""Regressão da falha Kotlin CI125: ícone LockOutline não é válido neste app.
Teste estrutural; não substitui compilação Gradle real.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = (root/'app/src/main/java/br/com/uaicortes/GeminiSettingsUi.kt').read_text(encoding='utf8')
assert 'Icons.Default.LockOutline' not in source, 'Símbolo indisponível no CI125'
assert 'Icon(Icons.Default.Lock, null' in source, 'Cadeado de credencial ausente'
assert 'import androidx.compose.material.icons.filled.*' in source
assert 'implementation("androidx.compose.material:material-icons-extended")' in (root/'app/build.gradle.kts').read_text()
assert 'Gemini · Conexão de IA' in source
assert 'Credencial ativa' in source
assert tuple(map(int, (root/'VERSION').read_text().strip().split('.'))) >= (0, 1, 149)
print('OK: CI125 — ícone do cadeado válido e painel Gemini mantido')
