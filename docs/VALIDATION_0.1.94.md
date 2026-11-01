# Validação v0.1.94 / versionCode 95 — 01/11/2026

## Escopo
- `SubtitleScreen.kt`: Play continua até o fim do projeto; mostra cues do instante real durante reprodução, sem usar `cue.endMs` para interromper.
- `SubtitlePreviewPolicy.kt`: política de intervalos [startMs,endMs), lacunas, sobreposição e texto em branco.
- `AudioScreen.kt` e `GeminiNarrationUi.kt`: atalho Gerar por IA no lugar de Extrair, com janela Gemini e painel Extrair áudio de vídeo mantidos.
- `SubtitlePreviewPolicyTest.kt`: testes JVM de intervalos sequenciais, lacunas, sobreposições e intervalos inválidos.

## Verificações
- `python3 tools/validate_source.py`: executar após modificações; verificações estruturais não equivalem a compilação.
- `python3 tools/ci70_kotlin_visibility_test.py` e `python3 tools/ci72_media3_optin_test.py`: regressões anteriores.
- `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`: exige SDK Android e dependências, executar em CI.

## Testes manuais pendentes
1. Selecionar legenda do meio, pressionar Play e verificar continuidade até o fim inclusive em clipes diferentes.
2. Confirmar troca do texto nas bordas exatas, pausas sem legendas e legendas com intervalos sobrepostos.
3. Pausar e arrastar a legenda selecionada; continuar reprodução e verificar comportamento após terminar o projeto.
4. Tocar em Gerar por IA, abrir e fechar janela, manter texto/configurações anteriores, e usar Extrair áudio de vídeo no painel separado.
5. Verificar renderização e redimensionamento da prévia para clipes verticais/horizontais e exportar MP4 de comparação.

Não presumir sucesso de build/aparelho nem APK publicado sem evidência.
