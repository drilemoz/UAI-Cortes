# Verificação do Uai Cortes 0.1.190 (versionCode 191)

- Fonte central: `GeminiVoiceCenterScreen`, reutilizada por Home, Vozes IA e Narração.
- Persistência: `GeminiVoiceLibrary` (última lista não vazia), `GeminiVoicePreviewCache` (arquivos duráveis e tamanho configurável).
- Identidade: versão de hash diferente da amostra anterior, sem misturar a frase antiga; voz/modelo/interpretação/texto fixo determinam a reutilização.
- Segurança: a primeira prévia exige confirmação de API/cota; seleção e abertura não chamam síntese.
- Validações: `tools/check_version_sync.py`, `tools/validate_source.py`, `tools/test_voice_center_190_contract.py`, testes Python atualizados e parser Kotlin PSI.

**Pendências**: Gradle completo no CI, instrumentação Android, chamadas Gemini em conta real, reprodução/retomada e layout com dispositivo.
