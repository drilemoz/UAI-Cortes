# Validação da v0.1.282

## Lógica

- `MusicTimelineTest` cobre duração real, início depois de zero, redução do projeto, trilha silenciada, ausência fora da linha do tempo e leitura de configurações antigas no schema 4.
- `tools/test_music_timeline_282_contract.py` valida a ligação do atalho com o pop-up, a separação dos trechos na timeline e a janela usada no preview e na composição.
- `MusicExportTimingAndroidTest` cria um WAV local e exporta um projeto curto, então decodifica o MP4 para localizar o áudio adicional, a música e o silêncio entre os dois.

```sh
gradle :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.uaicortes.MusicExportTimingAndroidTest
```

## Conferência em aparelho

1. Com uma narração já existente em Áudio, tocar em **Música**, escolher uma faixa, posicionar e aplicar. Verificar a nova pista e ouvir a narração e a música com volumes independentes.
2. Tocar no ícone da pista e no seu segmento. Ambos devem abrir o pop-up. Trocar o arquivo e cancelar, confirmando que a música anterior não foi alterada. Aplicar outra música, ajustar o início e usar Desfazer.
3. Partir de um projeto legado com música em repetição. Editar só o volume desde o início e confirmar que a repetição foi preservada. Ao importar música nova, ela não deve repetir.
4. Conferir as faixas compactas e as alças de vídeo/PiP/áudio com zoom 1× e 2× em tela pequena.
