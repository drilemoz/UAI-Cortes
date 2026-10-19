# Uai Cortes 0.1.18 | Validação

## Funcionalidade implementada no código
- Persistência de até quatro PiPs no campo `overlays`, `schemaVersion=4`, IDs estáveis; projetos com `pip` legado migrados em leitura.
- Quatro botões quadrados na mesma linha, Bottom Sheet ancorado embaixo, sobreposição dos controles sem deslocamento da prévia, nove atalhos, barra/entrada percentual, duração e mídia.
- Adição e substituição via MediaStore/SAF, controles independentes de volume e de silêncio.
- Layout anticolisão usa aspecto da mídia e do quadro, horário de início/fim e prioridade do PiP editado. Rejeita layouts impossíveis com Toast.
- Timeline individual por PiP e composição Media3 com entradas de vídeo ordenadas antes do vídeo base e faixas de áudio independentes.

## Testes disponíveis
- Parser Kotlin PSI: sintaxe dos arquivos Kotlin, não valida tipos/dependências Android.
- Smoke test Kotlin/JVM de geometria: quatro PiPs simultâneos, separação, reutilização temporal, bloqueio de layouts impossíveis e limite quatro.
- Testes JUnit adicionados para colisões, priorização, limites e migração JSON.
- `python3 tools/validate_source.py`: versão, workflow, referências a múltiplos PiPs e remoção dos diálogos PiP.
- ZIP: validação CRC, presença de fontes e documentação.

## Pendências antes de considerar validado
- Executar workflow `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` e verificar APK/assinatura.
- Testar física e visualmente: animação de expansão sem corte, gestos, layouts com PiPs grandes ou vídeos verticais, Tema Escuro/AMOLED.
- Testar prévia/exportação com 1, 2, 3 e 4 PiPs em vídeo; H.264/H.265, áudios opcionais, posições, proporção e sincronismo.
- A disponibilidade de 4 decodificadores simultâneos depende do hardware. Não afirmar compatibilidade antes de medir.
- Confirmar permissões de galerias (parcial e total) e URI SAF persistente após reiniciar.

## Inclusões 0.1.11 (11)
- Entrada numérica `TimeInput` com minutos, segundos, décimos e limites; testes para valores inválidos e overflow.
- `OverlayTiming.nextStart` baseado no último término, sem adiantar para dentro do PiP anterior; aviso quando o vídeo acaba.
- Colisão detectada por pares de PiPs e persistência `overlapWith` mediante confirmação explícita.
- Sem alça do ModalBottomSheet e margens compactas.
- Compilação Android em CI e teste físico permanecem necessários.

## Inclusões v0.1.11
- Testes unitários `NumericTimeTest` para máscara `mm:ss.d`, conversão e limites de corte.
- Editor de corte com preview ExoPlayer, seleção por dois marcadores e cursor; corte aplicado apenas por confirmação.
- **Ainda não validado:** compilação Android, resultado visual dos marcadores, preview e sincronismo em dispositivo físico.

## Inclusões v0.1.12
- Identificada causa de Marcar fim desabilitado: cursor coincide com início ou duração selecionada fica abaixo de 250 ms. Botão agora é clicável e mostra motivo.
- Miniaturas separadas da barra de cursor, com ação de toque; removido o RangeSlider que interceptava/encobria imagens.
- Entrada de cursor busca automaticamente no ExoPlayer durante digitação válida, sem botão aplicar.
- Ações de clipes numa linha, com snapshot de desfazer para aplicar, dividir, reordenar e excluir.
- Kotlin PSI e testes isolados verificam sintaxe e cálculos de horários; **não equivalem a Gradle Android, lint ou teste no telefone**.

## Inclusões v0.1.13
- Editor principal repete a playlist até pausar, tela Cortar e unir repete o clipe.
- Velocidades de prévia padronizadas com rótulos pt-BR; renderização do PiP no editor recebe a mesma velocidade.
- Mini tutorial abaixo da linha de ações da tela de corte, dentro da área rolável.
- **Ainda pendente:** compilação Gradle Android, teste de loop em fim de playlist, seleção de velocidade em dispositivo, áudio e sincronismo com vários PiPs.

## Inclusões v0.1.15
- Modelo JSON schema 4: `subtitles`, `transitions`, `audioTracks` opcionais na leitura. Mantidos `music`, `overlays` e migração `pip`.
- Extrator de áudio usa Media3 Transformer com `setRemoveVideo(true)` e grava arquivo AAC MP4 no URI SAF escolhido. M4A e MP4 são extensões sobre o mesmo contêiner; MP3 e WAV não estão incluídos.
- Fades RGB preto/branco por clipe via efeito temporal; sem crossfade. Legendas com `TextOverlay` por faixa temporal.
- Validações isoladas do parser SRT, cálculo de fade, limites de transição e PSI de Kotlin; não comprovam renderização final nem compatibilidade com codificadores do aparelho.
- Testar em CI: compileSdk36, `:app:testDebugUnitTest`, lint, assembleRelease, renderização legenda/fade, áudio AAC e permissões de escrita SAF, mais mixagem e volume dos PiPs.

## Build 15 / reparos v0.1.15

- Logs reais do GitHub Actions build 15, Debug e Release: `MainActivity.kt:656–657`, `1636–1640` exigiam opt-in de Material3; `MainActivity.kt:764` acessava `maxWidth` no receptor interno incorreto.
- `EditorScreen` e `PipScreen` anotados com `@kotlin.OptIn(ExperimentalMaterial3Api::class)` para manter separado o opt-in `androidx.annotation.OptIn` do Media3.
- `TimelineRow` registra `val timelineWidth = maxWidth` antes do `Row` aninhado.
- Testes locais estruturais e sintáticos apenas, sem Gradle Android. Nova execução `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` exigida no CI.

## Build 16 / implementação v0.1.18 (versionCode 19)
- Falha confirmada no build 16: task `:app:lintDebug`, 3 erros Media3 `UnsafeOptInUsageError` em `MainActivity.kt`, first failure line 1530. Kotlin e etapas anteriores concluídos, mas o release **não** foi publicado.
- Corrigida a origem das APIs instáveis: helper `previewPlayer` anotado com `androidx.annotation.OptIn` fora das lambdas `AndroidView`.
- `app/build.gradle.kts`: saídas para arm32/arm64/x86/x86_64/universal com mesmo versionCode 19.
- `tools/package_apks.py`: usa `output-metadata.json` e aborta se faltarem ABIs, houver duplicatas, falhar `apksigner verify` ou houver divergência de pacote/versão/SDK.
- Removidos do workflow `push.tags: v*` e `actions/upload-artifact` para evitar duas compilações/duas publicações do mesmo APK. A Release é o único destino.
- **Testes locais:** validador estrutural e sintaxe Kotlin (parser PSI) quando disponíveis; sem Gradle/SDK Android local.
- **Próximos testes em CI:** `testDebugUnitTest`, `lintDebug`, `assembleRelease`, verificação 5/5 APKs e assets de Release. Depois testar instalação e reprodução no ARM32, ARM64, x86, x86_64 e universal, sem presumir que haverá todos os aparelhos físicos.
