# Validação v0.1.139 · Vozes IA

## Alterações
- Tela independente `VoiceIaScreen`, rota `voices` e atalho do carrossel do editor.
- Faixa separada com tipo opcional `AudioTrack.kind` e metadados de origem/voz, mantendo o codec JSON no schema 4.
- Catálogo Gemini consultado apenas ao tocar, geração com consentimento, WAV salvo por fala e confirmação de inserção.
- `VoiceIaDraftStore` guarda checkpoint por projeto em arquivo JSON atômico; valida SHA-256 dos WAVs e diretório canônico antes de permitir reutilização.
- As legendas são sempre somente leitura neste fluxo.

## Testes automatizados locais
- `python3 tools/check_version_sync.py`
- `python3 tools/test_voice_ia_contract.py`
- `python3 tools/validate_source.py` e `tools/test_*.py`
- `app/src/test/java/br/com/uaicortes/VoiceIaPolicyTest.kt` (JUnit, rodar no Gradle).

## Pendências obrigatórias
- Compilar `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleRelease` no CI (5 APKs).
- Testar catalogação/geração em chave Gemini real e erro/cancelamento/retomada, vídeo vertical e horizontal, sobreposição, reabertura e backup/restauração de WAVs locais.
- Conferir áudio real em prévia, WAV e MP4 exportado; testes estáticos não equivalem a renderização e reprodução real.
