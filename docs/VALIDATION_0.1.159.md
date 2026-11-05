# Validação Uai Cortes v0.1.159

Implementação: Gemini Batch TEXTO em cenas Flow, vínculo local/fingerprint, consultas, revisão manual e parser REST de respostas inline.

Verificações locais a executar: `python3 tools/validate_source.py`, `python3 tools/check_version_sync.py`, `python3 tools/test_gemini_flow_batch_159_contract.py` e demais `tools/test_*.py`, análise sintática Kotlin/PSI, contrato JVM isolado da política, pacote ZIP/CRC.

**Pendências:** sem SDK Android/Gradle wrapper jar neste ambiente. Compilação/teses JUnit oficiais em GitHub Actions, chamadas reais Gemini Batch, processo longo/reabertura e revisão UI no Android ainda não executados. A API pode rejeitar modelos ou cobrar sob plano pago; teste somente com autorização da conta.
