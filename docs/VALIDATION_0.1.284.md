# Validação da v0.1.284

O log do workflow 241 confirma a compilação Kotlin de Debug e Release na v0.1.283 e a falha isolada em `VoiceIaSpeakerVoicePolicyTest.classicalGeneratedStyleMatchesSrtButKeepsWordTimingsAndSpeaker` (1 de 761 testes unitários). O cenário comparava uma legenda nova, cujo tamanho inicial passou a 14, com uma legenda convertida pedindo 12 explicitamente.

A v0.1.284 chama o parser SRT real, verifica o padrão 14 no estilo clássico e mantém as verificações de aparência, personagem e tempos das palavras. Um teste separado garante que escolher 12 explicitamente continua permitido. A lógica de produção não muda.

```sh
gradle --no-daemon :app:testDebugUnitTest --tests 'br.com.uaicortes.VoiceIaSpeakerVoicePolicyTest'
```

O workflow completo continua com testes, Lint e montagem das cinco variantes assinadas.
