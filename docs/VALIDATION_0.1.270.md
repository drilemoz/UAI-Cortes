# Validação 0.1.270 / versionCode 271

## Mudanças

- `ExportAudioPolicy`: filtra volume zero e calcula janela de origem/timeline sem inferir 1 segundo.
- `ExportMp4Inspector`: exige vídeo e duração; exige faixa AAC/outra faixa de áudio quando narração, música ou PiP sonoro estão ativos; valida antes/depois da publicação.
- `ExportSubtitlePolicy`: detecta conflitos temporais e limites de exibição sem alterar cues, com varredura eficiente O(n log n).
- `ExportScreen`: resumo de áudio e alertas antes da confirmação, sem prometer mixagem de áudio na prévia.
- `ExportQualityReport`: relatório factual de presença de faixa e duração do MP4 salvo; não verifica audibilidade nem texto dos pixels.

## Regressões

- Kotlin/JVM: `ExportAudioPolicyTest`, `ExportSubtitlePolicyTest`.
- Android instrumentado: `ExportPipelineSmokeAndroidTest.inspectorRecognizesRealVideoAndAudioFromFixture`, além dos testes de exportação já existentes.
- Python: `tools/test_export_audio_subtitle_270_contract.py`, demais contratos e `tools/validate_source.py`.

## Pendências

- Compilação Android Gradle no CI com SDK 36 e Secrets necessários.
- Testes físicos de 720/1080p, PiP, legenda e karaokê, áudio vinculado/música/narração em projetos longos e codecs variados; conferir se áudio é audível, não só a existência da trilha.
- Reproduzir importação de imagem inválida e `ERROR_CODE_MUXING_TIMEOUT` na captura de quadro em Xiaomi. São problemas anteriores, sem causa nova confirmada.
