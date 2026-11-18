# Validação da 0.1.309 — Sincronia das legendas no MP4

- Base v0.1.308. Identidade v0.1.309, versionCode 310. Schema 4, assinatura e cinco variantes preservados.
- Na amostra analisada, a abertura terminava em 2,000 s. A primeira fala aparecia escrita em 5,633 s no MP4, mas iniciava em 7,610 s no SRT. Outros blocos confirmaram adiantamento de aproximadamente 2 s.
- No Media3 1.11.1, `VideoEncoderGraphInput` acumula a duração dos itens e registra o offset no processador. `ExternalTextureManager` para vídeo e `BitmapTextureManager` para fotos adicionam esse offset antes de o `OverlayShaderProgram` passar o tempo do quadro ao overlay. O exportador não deve somar um segundo offset. A seleção da frase, o horário da palavra e a visibilidade usam o mesmo tempo. Ordem das camadas, transformações e mixagem permanecem iguais.
- `SubtitleExportClock309AndroidTest` prepara uma abertura de 2 s e um vídeo aparado com AAC, com e sem PiP. Decodifica o resultado para conferir a ausência de texto antes da fala, as duas palavras e a pausa, além do áudio. A renderização usa a composição de exportação em cache, sem publicar arquivo de teste na galeria.
- Os 174 scripts locais de regressão passaram, assim como a sincronização de versão e a validação do código. O Gradle não conseguiu baixar o plugin Android neste ambiente. APK e execução dos testes Android não foram validados localmente.

## Conferência no aparelho

1. Exportar novamente o projeto com a abertura de 2 s. Comparar as mesmas falas na timeline e no novo MP4. Não reutilizar o arquivo anterior.
2. Conferir também projetos sem abertura, com clipe dividido ou aparado e com PiP ou transição composta. Na existência de tempos por palavra, comparar o destaque em trechos antes e depois da junção.
3. Para a regressão automatizada no Android, executar `gradle :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.uaicortes.SubtitleExportClock309AndroidTest`.
