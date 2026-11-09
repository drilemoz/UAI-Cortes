# Validação v0.1.252 (2026-11-09)

## Falha real no workflow 209

A validação Python anterior passou. O Gradle chegou a `:subtitle-core:test` e compilou recursos do Android, mas `:app:compileDebugKotlin` falhou com `ExportScreen.kt:501:36 Unresolved reference 'InfoOutline'`. Assim, o workflow 209 **não concluiu** `:app:assembleRelease` nem produziu os cinco APKs.

## Correção aplicada

O ícone `Icons.Default.InfoOutline` da confirmação de exportação foi substituído pelo ícone `Icons.Default.Info`, já usado em outras telas com `androidx.compose.material.icons.filled.*`. Nenhum comportamento da exportação foi modificado. Incluído o teste `tools/test_ci209_export_info_icon_contract.py`.

## Verificações

- `tools/validate_source.py`: aprovado (validação estrutural).
- `tools/check_version_sync.py`: aprovado, `0.1.252/versionCode 253`.
- `tools/test_ci209_export_info_icon_contract.py`: aprovado; referência inválida não aparece mais nas fontes Kotlin do aplicativo.
- Outros 13 contratos direcionados concluídos com sucesso: CI128, CI203, v245, exportação compacta, CI147, exportação social, Redes 248/249, Facebook 250, Novidades 207, Atualizações 201, sincronização e CI115.
- `tools/release_notes_for_github.py`: aprovado, notas da versão atual geradas em UTF-8 com 590 bytes de conteúdo publicável.
- A bateria completa de 143 `tools/test_*.py` não terminou no ambiente local, apesar de os primeiros 60 terem passado. **Não declarar 143 aprovados.**
- Compilação Android/Media3 e lint Android não executados localmente (Gradle indisponível); próximo CI ainda precisa confirmar `:app:compileDebugKotlin`, `:app:assembleRelease`, `:app:lintDebug` e cinco APKs.
- Testes físicos no Android pendentes.

