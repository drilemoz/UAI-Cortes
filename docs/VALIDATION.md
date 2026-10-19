# Uai Cortes 0.1.14 | Validação

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

## Inclusões v0.1.14
- Modelo JSON schema 4: `subtitles`, `transitions`, `audioTracks` opcionais na leitura. Mantidos `music`, `overlays` e migração `pip`.
- Extrator de áudio usa Media3 Transformer com `setRemoveVideo(true)` e grava arquivo AAC MP4 no URI SAF escolhido. M4A e MP4 são extensões sobre o mesmo contêiner; MP3 e WAV não estão incluídos.
- Fades RGB preto/branco por clipe via efeito temporal; sem crossfade. Legendas com `TextOverlay` por faixa temporal.
- Validações isoladas do parser SRT, cálculo de fade, limites de transição e PSI de Kotlin; não comprovam renderização final nem compatibilidade com codificadores do aparelho.
- Testar em CI: compileSdk36, `:app:testDebugUnitTest`, lint, assembleRelease, renderização legenda/fade, áudio AAC e permissões de escrita SAF, mais mixagem e volume dos PiPs.
