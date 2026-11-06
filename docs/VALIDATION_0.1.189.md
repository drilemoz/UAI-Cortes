# Validação Uai Cortes 0.1.189 (versionCode 190)

## Escopo
- Preferência de interpretação Gemini por personagem e por legenda no mesmo JSON `voiceIaVoices`, sem mudar schemaVersion 4.
- Exceções de fala mantêm prioridade; escolher uma voz geral não apaga estilos, nem vice-versa. Remover personagem também apaga sua interpretação, sem remover falas.
- Prévia de voz/estilo usa uma frase neutra e apenas por confirmação explícita. WAV temporário em `cacheDir`, removido ao sair.
- Revisão de lote e geração de pendentes por personagem/filtro. Nenhuma consulta Gemini automática é iniciada por ajustes de configuração.
- Fila recuperada verifica voz, modelo e estilo; `STALE` bloqueia reenvio, `REVIEW` requer consentimento específico.

## Testes
- `python tools/check_version_sync.py`
- `python tools/validate_source.py`
- `python tools/test_voice_ia_characters_188_contract.py`
- `python tools/test_voice_ia_styles_189_contract.py`
- `python -m unittest discover -s tools -p 'test_*.py'`
- Kotlin PSI em classes novas e alteradas; testes JUnit `VoiceIaStylePolicyTest` e `VoiceIaBatchReviewPolicyTest` no Gradle.

**Pendências:** Gradle/Android CI, testes de instrumentação, Gemini com chave/cota real e teste de UI no celular. Testes locais de regras não comprovam funcionamento integral do APK.
