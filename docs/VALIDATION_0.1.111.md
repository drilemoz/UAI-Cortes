# Validação — Uai Cortes v0.1.111 / versionCode 112

## Alterações

- Modo **Por trechos** na janela Gemini a partir de seleção de legendas offline, online ou manuais.
- Preserva os horários absolutos da timeline, inclusive falas no fim do vídeo, sem usar o cursor da edição. Cada texto pode ser corrigido sem modificar as legendas originais.
- No máximo 12 solicitações de TTS em um lote, com confirmação de possível consumo de cota. Retentativas manuais reaproveitam somente os WAVs válidos de mesmo texto, voz, modelo e estilo na sessão.
- Revisão obrigatória por duração real: falas maiores que a legenda / sobrepostas precisam de concordância; fala que ultrapassa o fim do projeto bloqueia a inserção. Não há truncamento silencioso.
- Todas as faixas válidas são salvas em uma atualização do projeto; `schemaVersion=4` preservado.
- Modo contínuo mantém compatibilidade, mas não insere mais um arquivo cortado automaticamente no final da timeline.

## Testes locais

- `tools/validate_source.py` e `tools/check_version_sync.py`.
- `tools/test_narration_sync_contract.py` (contrato estático de UI, consentimento, geração e persistência).
- `app/src/test/java/br/com/uaicortes/SubtitleNarrationTimelineTest.kt` (JUnit: teste destinado ao GitHub Actions; build Gradle ainda pendente).
- Compilação Kotlin/JVM isolada de `SubtitleNarrationReuse.kt` e `SubtitleNarrationTimeline.kt` com cinco cenários executados no harness local.
- Parser Kotlin PSI sem erros sintáticos nos arquivos novos/modificados.
- Extração do ZIP e execução do validador dentro da cópia extraída.

## Não validado

- Build Android completo, Media3 áudio em várias faixas, geração Gemini real com chave, qualidade/latência do áudio e sincronismo do MP4 final em dispositivo. Não afirmar equivalência visual ou acústica até testar em aparelho.
- O modelo TTS não garante duração solicitada; a revisão apenas impede o corte silencioso e mostra o conflito.
