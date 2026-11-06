# Validação — Uai Cortes 0.1.187 / versionCode 188

## Falha reproduzida por inspeção do workflow 154
- Etapa `Testes, Lint e compilar Release`: `:app:compileDebugAndroidTestKotlin FAILED`.
- Arquivo `app/src/androidTest/java/br/com/uaicortes/HomeAboutPreviewAndroidTest.kt`, linha 5: `Unresolved reference 'assertDoesNotExist'`.
- Causa: import de topo inválido `androidx.compose.ui.test.assertDoesNotExist`. O método é acessível diretamente no `SemanticsNodeInteraction` devolvido por `onNodeWithText`.

## Alteração
- Removido exclusivamente o import inválido; as duas chamadas `.assertDoesNotExist()` permanecem no teste.
- Novo `tools/test_androidtest_assert_imports.py` para sinalizar imports de `assertDoesNotExist` ou `assertExists` indevidos e exigir asserções preservadas.
- Metadados, notas, CI e empaquetador sincronizados para v0.1.187/code188.

## Validação prevista
- Local: executar `python3 tools/check_version_sync.py`, `python3 tools/validate_source.py` e `python3 tools/test_androidtest_assert_imports.py`, e regressões estáticas do projeto.
- Obrigatória no CI: `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleRelease`.
- Teste instrumentado deve ser **executado no Android** posteriormente; compilar AndroidTest não significa executar AndroidTest.
- Em dispositivo: validar navegação Home/Sobre, verificar atualizações, salvamento, assinatura/atualização sobre instalação existente, e exportação de MP4.
