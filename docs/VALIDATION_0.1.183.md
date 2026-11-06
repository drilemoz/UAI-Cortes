# Uai Cortes 0.1.183 — Diarização Gemini

## Documentação de referência
- https://ai.google.dev/gemini-api/docs/transcribe — `transcription_config.mode` permite `diarization_mode: speaker` e `timestamp_granularities: [word]` combinados; speaker tags `spk_1` etc. Até oito vozes; três ou mais com identificação experimental.

## Proteções
- Somente `word_info` com timestamps reais são aceitos, sem criar horários fictícios.
- Rótulo ausente/inválido não ganha identidade, e mudanças de `speaker` quebram o grupo de palavras.
- ID provisório usa UUID por transcrição para nunca conectar pessoas entre vídeos diferentes.
- Revisão antes de aplicar: novo personagem, vincular existente, não atribuir; sem alterar cor de legenda, textos ou arquivos.
- Sem segunda chamada paga, sem diarização no Vosk, sem mudança `schemaVersion` 4.

## Testes
- `GeminiSubtitleParserTest`, `GeminiDiarizationPolicyTest`, `tools/test_gemini_diarization_183_contract.py` e regressão anterior.
- CI Gradle, testes Android e confirmação da resposta Gemini real pendentes. Não presumir precisão perfeita de diarização.
