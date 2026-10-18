# Uai Cortes 0.1.4

Editor Android offline em Kotlin, Jetpack Compose/Material 3 e Media3. Visual desenvolvido com base nas referências de tela fornecidas; o ícone está integrado nos recursos `mipmap-*`.

## Configuração Android

- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
- `versionName = 0.1.4`, `versionCode = 5`; APK de saída: **`UaiCortes-v0.1.4.apk`**.
- Kotlin 2.1.21, AGP 8.10.1, Gradle 8.11.1, Compose Material 3 e Media3 1.10.1.
- Edição e exportação local H.264/AAC; importação via Storage Access Framework e salvamento via MediaStore.

## Funcionalidades de desenvolvimento

- Início, projetos locais, editor de clipes, corte, divisão e ordenação.
- PiP de imagem/vídeo posicionável com gestos; controle de volume separado, mídia de áudio local e exportação 720p/1080p.
- Configurações, Tela e Sobre em módulos/telas, não em pop-ups.
- Transições: corte direto. Dissolvência/fade e corte fino de áudio ainda pendentes.
- Arquivos originais não são alterados; os projetos dependem das URIs dos arquivos escolhidos.

## Como baixar o APK do workflow

1. Envie **os arquivos extraídos deste ZIP** para a raiz da branch `main` do repositório.
2. O **push na main já inicia** `Gerar APK Uai Cortes`; alternativamente, execute manualmente em **Actions > Gerar APK Uai Cortes > Run workflow**.
3. Quando a execução terminar com sucesso, há **dois destinos obrigatórios**:
   - **Releases > v0.1.4 > Assets:** baixar diretamente `UaiCortes-v0.1.4.apk` sem ZIP.
   - **Actions > Gerar APK Uai Cortes > execução > Artifacts:** baixar `UaiCortes-v0.1.4-apk`, pacote ZIP automático do GitHub contendo o APK.
4. O resumo da execução (*Summary*) inclui o link direto de download do APK e seu SHA-256.
5. **Se o APK não for publicado em algum desses destinos, o job deve falhar** em vez de mostrar sucesso sem entrega.

## Origem do problema na 0.1.3

O workflow anterior não tinha `actions/upload-artifact` e a publicação no GitHub Releases usava:

```yaml
if: github.event_name == 'workflow_dispatch' || startsWith(github.ref, 'refs/tags/')
```

Logo, builds acionados por push na branch `main` terminavam verdes sem publicar nenhuma saída para download. A versão 0.1.4 remove esse bloqueio, adiciona o artifact a cada execução e valida a existência do asset na Release.

## Assinatura

Mantenha os quatro Secrets do repositório: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD`. O workflow carrega o keystore só no runner. Se faltarem, poderá produzir APK assinado com chave debug do runner que talvez não atualize versões anteriores. **Nunca commitar TXT de Secrets, keystore ou credenciais**.

## Verificações e limitações

- As compilações e o Lint desta **0.1.4 não foram executados localmente** (sem Android SDK/Gradle disponível); dependem de GitHub Actions.
- O workflow #3 da antiga 0.1.2 compilou Debug/Release e testes, mas falhou no Lint; a 0.1.3 corrigiu o opt-in experimental do Media3. A execução bem-sucedida relatada posteriormente ainda não foi disponibilizada em logs neste pacote.
- Media3, PiP, sincronização, codecs e renderização 720p/1080p precisam de testes físicos de exportação e estabilidade.
- Para mais detalhes, veja `docs/FEATURE_MATRIX.md`, `docs/VALIDATION.md` e `RELEASE_NOTES.md`.
