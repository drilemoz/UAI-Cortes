# Uai Cortes v0.1.84 / versionCode 85 — correção do CI 70

## Falha observada

Workflow **Gerar APK Uai Cortes 70**, com compilação `compileDebugKotlin` e `compileReleaseKotlin` reprovada em `ExportEngine.kt`, linhas 177 e 179: a função pública `start()` expunha os tipos internos `ExportBitratePreset` e `ExportQualityReport`.

## Correção

- Tornar **somente** `ExportEngine.start()` `internal` (é chamada pelo `internal class ExportTaskViewModel`, no mesmo módulo).
- Preservar `ExportBitratePreset` e `ExportQualityReport` internos e manter inalterado o contrato de callbacks e media pipeline.
- Atualizar verificador estrutural para prevenir retorno do problema.
- Sincronizar 0.1.84/85 em Gradle, workflow, empacotador, novidades, README, CHANGELOG e notas de release.

## Critérios de aprovação

1. Verificador: `python3 tools/validate_source.py`.
2. Teste isolado do compilador Kotlin para visibilidade (não equivale a build Android).
3. Validação sintática das fontes Kotlin e XML; integridade do ZIP.
4. **Pendente no GitHub Actions:** `:app:compileDebugKotlin`, `:app:compileReleaseKotlin`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleRelease`, verificação das cinco variantes assinadas e publicação.

Nenhuma chave da API ou da assinatura foi incluída no projeto. Não há comprovação de instalação/exportação em aparelho nesta entrega.
