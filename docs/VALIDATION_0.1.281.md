# Validação da v0.1.281

O campo opcional `textWatermark.clipId` mantém o JSON de projetos no schema 4. Sem o campo, a marca cobre o projeto inteiro. Se o clipe for movido, o intervalo acompanha seu ID. Se o clipe não existir mais, a marca deixa de aparecer até que outra duração seja definida. Padrões locais de estilo não carregam o vínculo de clipe.

## Verificações

- `TextWatermarkTest` cobre primeiro/último instante e corte, reordenação, clipe ausente, JSON legado e persistência das novas fontes.
- `WatermarkColorTest` cobre hexadecimal RGB e separação entre cor e opacidade.
- O teste isolado de captura `tools/test_frame_snapshot_project_266.py` verifica quando o print deve ou não incluir uma marca vinculada.
- `WatermarkTimingAndroidTest` exporta três clipes e confere os pixels com e sem PiP. A marca precisa aparecer no clipe do meio e ficar ausente nos demais.

```sh
gradle :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.uaicortes.WatermarkTimingAndroidTest
```

## Conferência visual no Android

1. Abrir projeto com quatro PiPs. Conferir os seletores em uma linha, alternar entre camadas e testar posição, tamanho digitado, duração e opacidade. No menu de opções, conferir troca de mídia e som em PiP de vídeo.
2. Abrir Marca d'água. A prévia deve ocupar o espaço livre sem cortar a imagem. Ampliar e voltar mantendo a posição de reprodução. Testar o controle de tempo com clipes de proporções diferentes.
3. Selecionar somente um clipe intermediário e usar a reprodução para verificar o início e a saída da marca. Reordenar o clipe e repetir a conferência no editor, na exportação e na captura de quadro.
4. Conferir as seis fontes, inclusive texto longo e acentos, na lista com amostra e no MP4. Trocar a cor pela paleta, testar uma cor hexadecimal e conferir a opacidade separadamente.
