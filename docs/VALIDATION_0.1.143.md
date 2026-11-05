# Validação v0.1.143 — Vozes IA: barra única e catálogos alfabéticos

- Base: ZIP v0.1.142, 444 arquivos; nova versão 0.1.143/versionCode 144.
- UI: barra única com seis controles (ícone Buscar, Lite/Flash, Voz geral, Humor/estilo, Gerar N e Inserir N); raio 8 dp, altura visual 40 dp, rótulos e descrições acessíveis, mantendo cartões detalhados e timeline.
- Catálogo de vozes e estilos classificados por `Collator` pt-BR, com rolagem interna até 320/310 dp e seleção visível; opções não removidas. Alterações individuais ou globais preservam confirmações e consentimento Gemini.
- Validação estrutural: `python3 tools/validate_source.py` aprovada, `tools/ci70_kotlin_visibility_test.py` e `tools/ci72_media3_optin_test.py` aprovadas.
- Regressões: 39 scripts `tools/test_*.py` executados com sucesso (6 processos em paralelo); novo teste `test_ci120_voice_ia_toolbar_contract.py`.
- **Pendente:** compilação Gradle/Android e testes em aparelho, inclusive comportamento responsivo de telas estreitas, acessibilidade, menu de estilos, TTS e exportação. Testes estáticos não provam APK funcional.
