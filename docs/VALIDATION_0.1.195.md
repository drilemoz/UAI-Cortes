# Uai Cortes 0.1.195 — Validação da correção CI161

## Diagnóstico

O workflow 161 falhou em `:app:compileDebugKotlin` e `:app:compileReleaseKotlin` por `Unresolved reference 'height'` em `ProjectDraftUi.kt`, linhas 44 e 48 da fonte v0.1.194. O método de extensão de `Modifier` existe no Compose Foundation, mas o arquivo não importava `androidx.compose.foundation.layout.height`.

## Alteração

Foi adicionado apenas o import ausente ao Kotlin da UI. O código de desenho, comportamento e decisão de saída do editor foi mantido. Novo teste `tools/test_ci161_project_draft_height_import.py` verifica a importação e os dois usos.

## Escopo de validação

- Verificação estrutural e versão com `tools/validate_source.py` / `tools/check_version_sync.py`.
- Regressão estática em `tools/test_*.py`, incluindo o teste novo.
- Kotlin PSI sintático no arquivo corrigido, quando disponível.
- Integridade ZIP/CRC e validação da fonte extraída.

**Pendente:** build Android real, testes unitários Gradle, instrumentados e lint no GitHub Actions. Não afirmar que a versão 0.1.195 compilou até o workflow completar com sucesso.

Versão: `0.1.195`, `versionCode 196`, sem mudança no schema de projeto.
