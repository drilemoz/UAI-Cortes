# Uai Cortes v0.1.2 (alpha, correção de compilação Media3)

- Corrige a falha observada no GitHub Actions **Gerar APK #2** durante `:app:compileDebugKotlin`.
- Atualiza o import de `OverlaySettings` de `androidx.media3.effect` para `androidx.media3.common`.
- Atualiza o import de `VideoCompositorSettings` de `androidx.media3.transformer` para `androidx.media3.common`.
- Elimina os erros decorrentes de interface `PipCompositor` não reconhecida pelos métodos `override`, conforme a referência oficial Media3 1.10.1.
- Inclui validador local de imports e sincronização de versão, executado antes do build no GitHub Actions.
- Mantém `compileSdk=36`, `targetSdk=36`, a interface, o ícone e os quatro Secrets de assinatura existentes, sem alterações.
- Atualiza `versionName=0.1.2`, `versionCode=3`, tela Sobre, documentação, tag `v0.1.2` e nome do APK para `UaiCortes-v0.1.2.apk`.
- O ZIP contém código-fonte; o APK é entregue individualmente por GitHub Releases, apenas após compilação bem-sucedida.
- **Pendente:** confirmar compilação Android, testes, Lint e exportação real em dispositivo, incluindo PiP e áudio. Fade/dissolvência continuam não implementados.
