# Validação Uai Cortes 0.1.193
- `python tools/check_version_sync.py`
- `python tools/validate_source.py`
- `python -m unittest discover -s tools -p 'test_*py'`
- Contratos UI v193 e teste Kotlin `VoiceIaCompactPolicyTest` (JUnit no CI).
- Compilação PSI Kotlin: arquivos alterados de interface/regra/testes.
- Conferência CRC/ZIP e GitHub Actions com builds Debug, instrumentados, 5 APKs Release assinados.
- Pendente: Gradle, testes instrumentados, UI em telas pequenas e chamadas Gemini reais.
