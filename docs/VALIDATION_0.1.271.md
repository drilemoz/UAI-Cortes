# Validação Uai Cortes 0.1.271 / versionCode 272

- Revisão dos riscos introduzidos na 0.1.270: contagem de PiP com volume ativo não significa faixa de áudio; legendas com início negativo podem estar visíveis no tempo 0.
- `ExportMp4Inspector.sourceHasAudio`: detecta áudio por MediaExtractor antes da composição PiP; sem áudio, não adiciona trilha nem exige áudio no MP4 por causa do PiP.
- `ExportSubtitlePolicy`: a interseção do intervalo da cue com `[0, project.durationMs)` define se ela é visível; alertas de colisão consideram tempos visíveis.
- Testes Kotlin/JVM: `ExportAudioPolicyTest.silentPipDoesNotCountAsVerifiedRequiredAudio`, `ExportSubtitlePolicyTest.cueStartingBeforeZeroButEndingAfterZeroIsVisible`.
- Teste Android instrumentado novo: `ExportPipelineSmokeAndroidTest.pipAudioProbeDifferentiatesVideoOnlyFromVideoWithAudio`; fixture MP4 sem áudio incluso.
- Inspeção do MP4 continua sendo de contêiner e duração: NÃO garante audibilidade, sincronização ou pixels da legenda. Build Gradle, CI e teste no aparelho pendentes.
