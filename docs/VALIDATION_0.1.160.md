# Validação Uai Cortes v0.1.160

Alterações: interface compacta de consumo, ajuda contextual, configuração unificada de limites e preços, referências públicas datadas e barras de sucesso semanticamente corretas.

Fontes de referência conferidas em 05/11/2026: https://ai.google.dev/gemini-api/docs/pricing e https://ai.google.dev/gemini-api/docs/rate-limits. Tarifas são Standard em USD por milhão de tokens; alguns modelos têm atualização em 01/01/2027. Referência não é leitura em tempo real e não informa plano ou cobrança.

Testes locais: a preencher após execução. Full Gradle/Android/Compose e interação com teclado/testes de aparelho continuam pendentes até serem executados no CI.

## Resultados locais em 05/11/2026
- 56/56 scripts `tools/test_*.py` aprovados em execução paralela.
- `tools/validate_source.py` aprovado.
- `tools/check_version_sync.py` aprovado com versionCode 161.
- PSI Kotlin: 5/5 arquivos alterados/com atualização de metadados sem erros sintáticos.
- Teste JVM isolado `GeminiPublicPriceCatalog`: referência em 2026, atualização de 2027, modelo não cadastrado e bloqueio por desatualização aprovados.
- Não foi possível executar Gradle/Android completo por ausência do SDK e da distribuição do wrapper neste ambiente. UI Compose/teclado/ação Android e valores de preços atuais deverão ser validados no aparelho e página oficial.
