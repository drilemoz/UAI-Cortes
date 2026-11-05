# Validação v0.1.142 — compactação de Vozes IA

- Código base: ZIP v0.1.141, workflow CI118. Sem alteração das fontes de vídeo, legendas, segurança de Gemini ou mixagem.
- Escopo UI: seleção de voz geral e humor/estilo geral, confirmação de aplicação em lote, cartões compactos, tempo decimal, alerta de sobreposição temporal e prévia compartilhada.
- Verificações locais: `python3 tools/validate_source.py`; `python3 tools/test_ci119_voice_ia_compact_ui_contract.py`; `python3 tools/test_*.py` (todas); Kotlin `VoiceIaDisplayPolicy.kt` em JVM quando compilador disponível; integridade de ZIP via `tools/package_source.py`.
- **Pendências:** compilação Gradle Android, JUnit de todos os módulos, inspeção da interface e testes físicos do Gemini e exportação. Suíte estática não garante APK compilado.
