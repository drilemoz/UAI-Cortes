# Uai Cortes v0.1.3 (alpha, correção Android Lint Media3)

- Corrige a falha confirmada no GitHub Actions **Gerar APK #3** durante `:app:lintDebug`: dois erros `UnsafeOptInUsageError` ao usar `PlayerView.setShowBuffering()` e a constante `SHOW_BUFFERING_WHEN_PLAYING`.
- Adiciona `import androidx.annotation.OptIn` e `import androidx.media3.common.util.UnstableApi` e aplica `@OptIn(UnstableApi::class)` **somente** à função `EditorScreen`. Não usa `kotlin.OptIn`, conforme orientação oficial Media3.
- Mantém o indicador de buffering visível, o Android Lint habilitado e nenhuma supressão geral de erros.
- Atualiza `versionName=0.1.3` e `versionCode=4`, tela Sobre, README, validação de fonte, workflow, tag `v0.1.3` e APK `UaiCortes-v0.1.3.apk`.
- Preserva Android API 36, ícone, interface, Media3 e o mecanismo de assinatura pelos quatro Secrets existentes.
- O workflow #3 comprovou compilação Debug e Release, testes unitários e `:app:assembleRelease` na 0.1.2; apenas o Lint impediu que o job chegasse à publicação. O ZIP desta entrega é apenas o código-fonte.
- **Pendente:** validar a compilação/Lint do 0.1.3 no GitHub Actions, publicação do APK em Release Assets e execução em dispositivo físico, incluindo PiP/áudio. Fade/dissolvência e corte fino de áudio ainda não estão implementados.
