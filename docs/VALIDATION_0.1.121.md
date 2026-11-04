# Validação da versão 0.1.121 — workflow 100

## Evidência recebida
- Logs de GitHub Actions `Gerar APK Uai Cortes 100`, versão 0.1.120.
- A tarefa `:app:compileDebugAndroidTestKotlin` falhou em `HomeAboutPreviewAndroidTest.kt:5:33` com `Unresolved reference 'assertExists'`.
- O Kotlin principal e o Debug Java compilaram nos logs; os warnings `Icons.Default.*` são não fatais.
- O build parou antes de `:app:assembleRelease` e da publicação dos cinco APKs.

## Correção
- Removido somente o import inválido `import androidx.compose.ui.test.assertExists`; `onNodeWithText(...).assertExists()` continua válido como método de `SemanticsNodeInteraction`.
- Adicionado `tools/test_ci100_compose_import_contract.py` ao conjunto já executado automaticamente por `tools/test_*.py`.
- Atualizados arquivos de versão, CI, release notes, Home/Sobre, GitHub Manager e CHANGELOG.

## Critério de aprovação ainda pendente
- Executar no GitHub Actions: `gradle --no-daemon :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleRelease`.
- Confirmar que `compileDebugAndroidTestKotlin` concluiu sem erros, e que os cinco APKs assinados foram publicados em Release Assets.
- Executar testes instrumentados de UI e MP4 em aparelho/emulador compatível: compilar AndroidTest não os executa.
- Não declarar compilação Android completa aprovada com base apenas nos testes Python locais.
