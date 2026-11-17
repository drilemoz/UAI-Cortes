# Validação da v0.1.280

## No código

- `QuickAudioPlacementTest`: percurso contínuo, horário local, cortes exatos e limite do projeto. Selecionar o áudio não cria faixa. A cópia de segurança e a colocação acontecem ao aplicar.
- `OverlayEditorLogicTest`: margem simétrica de 4% em quadros 9:16, 16:9 e quadrados. Sem margem forçada no arraste nem migração de coordenadas salvas. O atalho usa o quadro da prévia atual. Um formato diferente escolhido na exportação pode mudar o enquadramento e precisa ser conferido separadamente.
- `PipExportQualityAndroidTest`: pixel real de MP4 720×1280 e 1080×1920. Compara margem, transparência do PNG e separação de traços finos de um PNG e um PiP de vídeo. O MP4 de teste é curto, H.264, sem áudio, com sete traços brancos sobre fundo verde (320×160). Não está no APK de produção.

## No Android

Executar com aparelho conectado ou emulador com OpenGL e encoder H.264:

```sh
gradle :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.uaicortes.PipExportQualityAndroidTest
```

Também conferir em tela pequena e fonte ampliada:

1. Abrir Camadas e alternar PiP/Marca d'água. Verificar os quatro cartões, rolagem quando necessário e rodapé sempre disponível.
2. Aplicar os quatro cantos do PiP e conferir a margem na prévia e no MP4. O arraste continua podendo alcançar a borda.
3. Usar quatro clipes de 10 s e um arquivo de 26 s. O pop-up deve informar o fim em 00:06 do vídeo 3. Cancelar e confirmar que nenhuma faixa foi criada. Aplicar, ouvir as transições entre clipes e conferir desfazer/refazer.
4. Com o cursor perto do fim, selecionar áudio mais longo que o tempo restante. Checar o aviso antes de aplicar e conferir que nenhuma mídia original foi cortada.
5. Reabrir uma edição interrompida e conferir a posição dos dois botões. Em Vozes IA, revisar o lote com uma fala maior que seu intervalo e conferir a identificação da legenda.
