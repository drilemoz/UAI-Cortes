# Uai Cortes v0.1.1 (alpha, correção do workflow)

- Corrige falha do GitHub Actions #1 ao instalar o pacote legado `tools` do Android SDK.
- Atualiza `android-actions/setup-android` para v4 com `packages: 'platform-tools'`, instala e verifica API 36 e Build Tools 36.0.0.
- Mantém o nome individual de instalação `UaiCortes-v0.1.1.apk`, publicado diretamente em GitHub Releases, sem ZIP para o APK.
- Atualiza `versionName=0.1.1`, `versionCode=2` e a página Sobre, sem alterar a interface ou o motor de vídeo.
- A primeira falha ocorreu **antes de executar o Gradle**; compilação Kotlin, Lint, assinatura e exportação em dispositivo real ainda não foram confirmados.
- Fade/dissolvência continuam não implementados; somente corte direto está previsto na alpha.
