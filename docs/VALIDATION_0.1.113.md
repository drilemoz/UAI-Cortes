# Validação — Uai Cortes v0.1.113 / versionCode 114

## Objetivo

- Exibir forma de onda RMS e sugerir possíveis regiões de fala na referência selecionada sem chamada externa.
- Executar somente após toque em **Analisar região desta fala**; por padrão nenhum decoder extra é iniciado.
- Permitir analisar vídeo da timeline, faixa de áudio existente ou arquivo SAF, respeitando offsets e cortes do projeto.
- Janela próxima da legenda limitada a 15 segundos, análise assíncrona no Dispatcher.IO, limite de 20 s por origem, cancelamento e liberação dos recursos nativos.
- Sugerir um início apenas perto da legenda selecionada, sem alterar legendas nem faixas automaticamente. Duas confirmações: Usar sugestão e Aplicar horários.
- Heurística por energia sonora; música, efeitos e ruído podem causar falsos positivos. Não é reconhecimento de voz, diarização, nem marcação palavra a palavra.

## Testes locais

- `python3 tools/test_reference_activity_contract.py`
- `python3 tools/check_version_sync.py`, `python3 tools/validate_source.py`
- Teste Kotlin puro (`ReferenceAudioActivityTest.kt` e amostras sintéticas), análise sintática dos arquivos Kotlin alterados, integridade CRC do ZIP e validação após extração.

## Pendente

- Build Gradle Android em CI, testes reais com arquivos MP4/AAC/WAV, mídia sem faixa de áudio, múltiplos clipes, offsets de faixa/arquivo externo e permissões SAF.
- Avaliar consumo/temperatura em celular de entrada e falsos positivos com música/efeitos.
- Verificar se as sugestões manuais resultam em áudio Gemini sincronizado na exportação MP4; não prometer precisão automática.
