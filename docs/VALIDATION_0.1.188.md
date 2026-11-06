# Validação — Uai Cortes 0.1.188 / versionCode 189

## Alterações
- Preferências de voz por personagem e legenda em `VideoProject.voiceIaVoices`, com persistência por rascunho e compatibilidade JSON de `schemaVersion=4`.
- Precedência individual > personagem > geral > vozes legadas, sem reconhecimento de identidade ou chamadas de API na configuração.
- Cartões compactos, faixa de personagens, filtro/busca que não restringe a geração, prévia expansível.
- Fila pendente fica STALE quando a voz selecionada muda; WAVs anteriores e faixas inseridas são preservados.
- Presets de aparência Clássica (SRT) e Karaokê IA com `wordTimings` preservados.

## Verificações locais
- `python tools/check_version_sync.py` e `python tools/validate_source.py`: aprovados.
- 84/84 scripts Python de regressão (`tools/test_*.py`): aprovados.
- Teste CI154 de imports AndroidTest: aprovado, 14 arquivos instrumentados inspecionados.
- Parser PSI Kotlin: oito arquivos alterados sem erros sintáticos.
- Compilação Kotlin/JVM ISOLADA das políticas novas, com stubs dos modelos/JSON: aprovada; 13 verificações funcionais aprovadas. **Não equivale à compilação Gradle do app.**
- Oito testes JUnit adicionados em `VoiceIaSpeakerVoicePolicyTest.kt`, mas não executados sem Gradle.
- ZIP verificado estruturalmente e por CRC depois de criado.

## Pendências antes do APK
- Executar no GitHub Actions: `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleRelease`.
- Instalar no Android e testar filtro, tamanho das fontes, seleção de vozes, retorno ao editor, Salvamento/Descartar e sincronismo do WAV.
- Confirmar em aparelho a aparência e MP4 dos estilos; não inferir que teste estático equivale a compilação.
