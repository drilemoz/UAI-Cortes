# Uai Cortes 0.1.149 — validação da correção CI125

- **Falha observada:** workflow 125 na v0.1.148, `GeminiSettingsUi.kt:130:40 Unresolved reference 'LockOutline'`, Debug/Release Kotlin.
- **Correção aplicada:** `Icons.Default.LockOutline` → `Icons.Default.Lock` com a dependência `material-icons-extended` já presente.
- **Escopo:** apenas ícone e metadados/versionamento, sem alteração de API, segredos, persistência, áudio, exportação ou schema dos projetos.
- **Verificações:** `tools/test_ci125_gemini_lock_icon_contract.py`, `tools/test_ci148_gemini_ui_redesign_contract.py`, `tools/validate_source.py`, `tools/check_version_sync.py` e outros testes Python; os resultados precisam ser registrados após execução.
- **Pendências:** `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleRelease` no GitHub Actions; instalação e inspeção visual em Android real.
