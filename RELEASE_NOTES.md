Correção v0.1.20: o workflow 19 compilou Kotlin Debug/Release, executou testes e lint com sucesso, mas falhou no empacotamento porque tools/package_apks.py ainda esperava 0.1.17/versionCode 17. Constantes sincronizadas para 0.1.20/versionCode 21. Compilação e publicação da nova versão pendentes.

## Correção v0.1.20 (versionCode 21)
- Corrige o erro `No value passed for parameter onClip` em `MainActivity.kt:825` encontrado no build 18.
- Compilação Android e teste físico ainda pendentes.

Implementação incremental: indicadores de transição clicáveis na timeline, seleção por cartões e botão salvar e voltar à Home. Prévia real de transições ainda pendente.

# Uai Cortes v0.1.20 · versionCode 21

## Correção do build 16
- Resolve uso de API `UnstableApi` do Media3 dentro das lambdas `AndroidView` que fazia `:app:lintDebug` falhar. Configurações de vídeo centralizadas em função com `@androidx.annotation.OptIn`.
- Mantém editor, quatro PiPs, legendas, cortes, áudio, fades e efeitos já implementados.

## Um Release com cinco APKs para baixar
- `UaiCortes-v0.1.20-arm32.apk` (ARM 32-bit, `armeabi-v7a`)
- `UaiCortes-v0.1.20-arm64.apk` (ARM 64-bit, `arm64-v8a`)
- `UaiCortes-v0.1.20-x86.apk` (Intel x86 32-bit)
- `UaiCortes-v0.1.20-x86_64.apk` (Intel x86 64-bit)
- `UaiCortes-v0.1.20-universal.apk` (universal, indicado quando não souber a ABI)

O CI publica **somente os cinco APKs Release** como arquivos diretos em GitHub Releases, não repete o pacote no Actions Artifacts e não reexecuta automaticamente por causa de tag criada pelo Release.
Todas as variantes usam o mesmo versionCode e a mesma assinatura do workflow. Utilize apenas uma por instalação.

## Limitações e validação
- Android Lint e compilação completa ainda precisam ser confirmados no próximo GitHub Actions.
- As variantes por ABI podem ser semelhantes em tamanho quando o aplicativo não embarca bibliotecas nativas específicas.
- Fades, exportação de múltiplos PiPs, SRT e extração AAC ainda precisam de validação em aparelhos reais. Dissolvência, MP3 e WAV não estão disponíveis.
