# Uai Cortes — validação v0.1.157 (05/11/2026)

## Escopo implementado
- `GeminiUsageAnalytics.kt`: projeções opcionais USD por milhão de tokens de entrada e saída definidas pelo usuário, separadas por projeto/modelo. Custo parcial somente com ambas as contagens informadas e operação concluída; nunca representar cobrança oficial.
- `GeminiPricingUi.kt`: configuração manual e gráficos de tentativas, taxa de sucesso, latência média de chamadas reais por operação e atividade diária local.
- `GeminiUsageUi.kt`: integração na Central Gemini e aviso de custo aproximado antes de lotes semelhantes após pelo menos cinco medições completas.
- `GeminiUsageLedger.kt` e clientes TTS/legendas/descrição: duração da requisição capturada pelo relógio monotônico em milissegundos, sem prompts, mídia ou credenciais.

## Verificações locais
- `python3 tools/check_version_sync.py`: aprovado (versão 0.1.157, versionCode 158).
- `python3 tools/validate_source.py`: aprovado.
- 53/53 scripts `tools/test_*.py` aprovados em execução paralela, inclusive o novo contrato 157.
- Parsing PSI Kotlin de 8 arquivos alterados e do novo teste JUnit: aprovado, sem erros sintáticos.
- Compilação JVM isolada do `GeminiUsageAnalytics.kt` com stubs de Android/JSON e testes de preços, ausências, erros, agregação e latência: aprovada.

## Limitações explícitas
- O build Android/Gradle, a execução instrumentada e o teste físico da interface **não** foram realizados neste ambiente. O JUnit Android completo permanece pendente do CI.
- Tarifas dependem da região, tier, tipo de operação, modalidade e eventuais descontos/cache. O painel nunca estima preço quando faltam tokens de entrada ou saída e não consulta cobranças reais.
- A retenção atual de até 500 eventos/90 dias limita os gráficos; taxas de sucesso e custos refletem somente eventos capturados por este aparelho.
- As durações armazenadas medem a interação HTTP monitorada; não são tempo de exportação nem tempo total de preparação local. Dados antigos continuam sem duração.
- Preservados schemaVersion 4, cinco APKs Release no CI e proteção de chaves.
