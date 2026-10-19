## v0.1.23 (versionCode 24) · 19/10/2026

- Faixas adicionais com entrada e saída no áudio de origem, posição na timeline, controles numéricos/deslizantes e pré-escuta separada.
- Exportação Media3 e prévia do editor respeitam `sourceStartMs`; projetos antigos permanecem compatíveis com origem em 0 ms.
- Limites de corte conservadores (mínimo de 250 ms) e testes de lógica/serialização adicionados.
- ZIP/validação estrutural: a compilação Android e teste físico desta versão continuam pendentes.

## v0.1.22 (versionCode 23) — 19/10/2026

- Pré-escuta de áudio para clipes individuais e vídeos externos, com reprodução, pausa, reinício e limite de recorte.
- Reprodução temporária isolada da timeline e liberação do player ao sair da tela.
- Pré-escuta de projeto inteiro ainda não implementada. Build Android e testes em aparelho pendentes.

## v0.1.22 (versionCode 23) — 19/10/2026

- Prévia animada de transições usando quadros reais extraídos dos clipes adjacentes, com Fade preto, Fade branco e Corte direto.
- Extração de quadros em IO, sem decodificação contínua ou áudio. A prévia não substitui reprodução/renderização integral.
- Mantidas as cinco variantes de APK e a exportação Media3 existente.
- Build Android e testes em aparelho pendentes.

Correção v0.1.22: o workflow 19 compilou Kotlin Debug/Release, executou testes e lint com sucesso, mas falhou no empacotamento porque tools/package_apks.py ainda esperava 0.1.17/versionCode 17. Constantes sincronizadas para 0.1.22/versionCode 23. Compilação e publicação da nova versão pendentes.

## Correção v0.1.22 (versionCode 23)
- Corrige o erro `No value passed for parameter onClip` em `MainActivity.kt:825` encontrado no build 18.
- Compilação Android e teste físico ainda pendentes.

Implementação incremental: indicadores de transição clicáveis na timeline, seleção por cartões e botão salvar e voltar à Home. Prévia real de transições ainda pendente.

# Uai Cortes v0.1.22 · versionCode 23

## Correção do build 16
- Resolve uso de API `UnstableApi` do Media3 dentro das lambdas `AndroidView` que fazia `:app:lintDebug` falhar. Configurações de vídeo centralizadas em função com `@androidx.annotation.OptIn`.
- Mantém editor, quatro PiPs, legendas, cortes, áudio, fades e efeitos já implementados.

## Um Release com cinco APKs para baixar
- `UaiCortes-v0.1.22-arm32.apk` (ARM 32-bit, `armeabi-v7a`)
- `UaiCortes-v0.1.22-arm64.apk` (ARM 64-bit, `arm64-v8a`)
- `UaiCortes-v0.1.22-x86.apk` (Intel x86 32-bit)
- `UaiCortes-v0.1.22-x86_64.apk` (Intel x86 64-bit)
- `UaiCortes-v0.1.22-universal.apk` (universal, indicado quando não souber a ABI)

O CI publica **somente os cinco APKs Release** como arquivos diretos em GitHub Releases, não repete o pacote no Actions Artifacts e não reexecuta automaticamente por causa de tag criada pelo Release.
Todas as variantes usam o mesmo versionCode e a mesma assinatura do workflow. Utilize apenas uma por instalação.

## Limitações e validação
- Android Lint e compilação completa ainda precisam ser confirmados no próximo GitHub Actions.
- As variantes por ABI podem ser semelhantes em tamanho quando o aplicativo não embarca bibliotecas nativas específicas.
- Fades, exportação de múltiplos PiPs, SRT e extração AAC ainda precisam de validação em aparelhos reais. Dissolvência, MP3 e WAV não estão disponíveis.
