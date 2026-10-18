# Uai Cortes 0.1.2

Editor offline Android nativo em Kotlin + Jetpack Compose + Media3.

## Tecnologias
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
- Kotlin 2.1.21; AGP 8.10.1; Gradle 8.11.1; Compose Material 3.
- Media3 1.10.1 para prévia ExoPlayer e exportação Transformer em H.264/AAC.
- Importação de arquivos por `ACTION_OPEN_DOCUMENT`, com permissões persistentes apenas nos arquivos escolhidos.
- Exportação para `Movies/UaiCortes` via MediaStore, sem INTERNET e sem acesso amplo ao armazenamento.

## Telas incluídas
1. Início (cartão azul de Nova edição, edições recentes, barra inferior).
2. Projetos locais persistentes.
3. Editor com prévia, lista de clipes, faixas PiP/áudio, execução e posição no tempo.
4. Cortar, dividir, reorganizar ou remover clipes.
5. PiP imagem/vídeo: posição por arrasto, escala por pinça, duração e áudio independente.
6. Áudio: volume original, silenciar, inclusão de música local, volume separado; silêncio do primeiro clipe.
7. Transições: corte direto. Fade/dissolvência ainda não implementados.
8. Exportação 720p/1080p com progresso, cancelamento e salvamento na galeria.
9. Configurações em páginas completas: Geral, Tela, Sobre.

## Histórico de correções do GitHub Actions
- A primeira execução (18/10/2026) falhou no passo `SDK Android`, antes do Gradle.
- Causa comprovada: `android-actions/setup-android@v3` tentava instalar `sdkmanager tools`, um pacote removido do repositório Android.
- Ajuste: `android-actions/setup-android@v4` com `packages: 'platform-tools'`, instalação explícita da API 36 e ferramentas 36.0.0, e validação dos executáveis `apksigner`/`aapt`.
- No workflow #2 (18/10/2026), o Android SDK 36, Gradle e assinatura foram preparados corretamente, mas `:app:compileDebugKotlin` falhou por dois imports Media3 desatualizados em `ExportEngine.kt`.
- A versão 0.1.2 aponta `OverlaySettings` e `VideoCompositorSettings` para `androidx.media3.common`, conforme a documentação da API. O resultado do novo build ainda precisa ser validado.

## Como gerar APK
1. Envie os arquivos da raiz para um repositório GitHub (não coloque o ZIP dentro do repositório).
2. Abra **Actions > Gerar APK Uai Cortes > Run workflow**.
3. O workflow roda testes, Lint e `assembleRelease` com API 36.
4. Após sucesso, a **Release v0.1.2** conterá o arquivo individual `UaiCortes-v0.1.2.apk` em *Assets* (não um ZIP do APK).

### Assinatura
Use os Secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` para assinatura estável de Release (o workflow cria a keystore privada somente no runner). Se não forem informados, é usada uma chave DEBUG gerada no runner: útil apenas para testes, **não garante atualização entre builds**. Nunca coloque a keystore no ZIP ou no GitHub.

## Limitações e pontos a validar no dispositivo
- O projeto foi elaborado sem compilação Android local nem dispositivo de teste disponíveis. Não afirmar que o APK está pronto ou testado antes de passar no Actions.
- Media3 Transformer tem limitações documentadas para crossfade; por isso o menu **Transições** só ativa corte direto nesta versão.
- PiP simultâneo e mistura de áudio dependem dos codecs disponíveis e precisam de testes reais de exportação em aparelhos diferentes. A prévia PiP usa duas instâncias ExoPlayer aproximadas, podendo apresentar defasagem.
- Os projetos guardam URIs persistentes, não cópias: se o arquivo original for movido ou excluído, não será possível reabrir sua mídia.
- Renderização de vídeos longos pode aumentar consumo, temperatura e espaço temporário ocupado; exportação ainda não usa Foreground Service.

## Ícone
Ícone azul original do Uai Cortes integrado em `mipmap-*/ic_launcher.png`, além do arquivo mestre `design/icone-uai-cortes.png`.

## Versionamento
VersionName `0.1.2`, versionCode `3`; arquivo de APK previsto `UaiCortes-v0.1.2.apk`.
