# Validação Uai Cortes v0.1.264 / versionCode 265

- Origem: fonte v0.1.263 e logs workflow 221, 09/11/2026.
- Causa: `ProjectDraftUi.kt` usava `Surface` nos diálogos `ConfirmRecoverDraft` e `InvalidEditorDraft` sem `import androidx.compose.material3.Surface`.
- Consequência no CI: `Unresolved reference 'Surface'` e mensagens `@Composable invocations can only happen from the context of a @Composable function`, levando a `:app:compileDebugKotlin FAILED`.
- Correção: import Material 3 adicionado, sem modificar as ações ou os dados de rascunho.
- Regressão: `tools/test_ci221_project_draft_surface_contract.py` confere import e preservação dos diálogos.
- Validar novamente `tools/validate_source.py`, sincronismo das versões, testes direcionados e ZIP oficial. Compilação Android completa e teste no aparelho pendentes.
