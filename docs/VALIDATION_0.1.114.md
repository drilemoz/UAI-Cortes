# Validação — Uai Cortes v0.1.114 / versionCode 115

## Escopo
- Comparação local A/B do vídeo original versus WAV Gemini preparado por fala (não é reprodução simultânea nem clonagem de voz).
- Meta de duração por fala em passos de 250 ms; regeneração usa instrução aproximada, sem edição forçada do WAV.
- Confirmação explícita de cota e substituição da referência apenas após recebimento/salvamento bem-sucedido.
- Retém WAV anterior em erro, sem modificar legendas originais ou esquema de projeto.
- Corrige a classificação de WAV curto demais versus áudio que ultrapassa o fim do projeto.

## Verificações locais
- `tools/validate_source.py` e `tools/check_version_sync.py` em código e ZIP extraído.
- PSI Kotlin de `GeminiNarrationUi.kt`, `GeminiNarrationComparisonUi.kt` e `NarrationDurationAssist.kt`.
- Casos Kotlin/JVM puros de limites e instruções de meta de duração.
- Contrato de interface `tools/test_narration114_contract.py`.

## Pendências
- Compilação Gradle/Android completa; testes Compose, Media3 ExoPlayer com múltiplos clipes e arquivos sem áudio; chamadas Gemini reais; revisão e sincronização do MP4 exportado em celular.
