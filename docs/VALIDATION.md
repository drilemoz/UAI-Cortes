## v0.1.42 (versionCode 43) · 27/10/2026
- Exportação de legendas: `TimedSubtitleOverlay` respeita largura máxima de 90% do quadro, com quebras de linha automáticas e tamanho em proporção à largura real do vídeo, em vez de bitmap horizontal ilimitado. Preserve tamanho, posição, efeitos e tempos originais do projeto.
- `TextOverlay.getBitmap` substituído por StaticLayout com largura controlada e cache por quadro; legenda sem texto continua transparente, protegida contra imagem 0×0.
- Sucesso de exportação em tela cheia, somente após salvar no MediaStore, com Abrir, Compartilhar, Voltar ao editor e Fechar; falhas continuam detalhadas na tela de exportação.
- Não foi possível realizar exportação MP4 em aparelho neste ambiente; confirmar fidelidade da prévia e os efeitos no vídeo físico, vertical e horizontal.

## v0.1.41 (versionCode 42) · 21/10/2026
- Nova seção **Configurações > Diagnóstico**, desligada inicialmente e ativada manualmente, para capturar data e hora, tela ativa, toques aproximados (grade de 5%) e ações de navegação/salvamento.
- Captura falhas não tratadas, erros Media3 dos players, código/causa categorizada da exportação, extração de áudio e falhas ao salvar o relatório; logs internos limitados a 512 KiB com rotação.
- Visualização local, exportação TXT pelo seletor SAF, compartilhamento explícito pelo Android Sharesheet com FileProvider e exclusão com confirmação. Nenhum upload automático.
- Relatórios omitem textos digitados, legenda, nomes e URIs de mídia, IDs de projetos e conteúdo de arquivos; incluem modelo do aparelho/Android quando exportados. Modo não substitui o Logcat integral ou testes reais no dispositivo.
- Sem mudanças no schemaVersion 4, algoritmos de corte e codificação, projetos salvos e segredos. Build Android e testes físicos pendentes.

## v0.1.40 (versionCode 41) · 21/10/2026
- Corrigida provável causa do erro de exportação Media3 `ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED` com `width and height must be > 0`: `TimedSubtitleOverlay.getText()` retornava string vazia nos intervalos sem legenda e no primeiro frame de `Letra por letra`. O `TextOverlay` do Media3 tenta criar bitmap com largura zero nesse caso.
- A exportação usa caractere NBSP transparente de largura positiva quando nenhum texto deve aparecer, e define alpha 0 no overlay inativo; nenhum caractere adicional é gravado nos textos ou arquivos SRT.
- Acrescentados testes de regressão para legendas vazias, espaços/quebras de linha e texto visível. Mensagem de erro específica para falha de composição de quadro.
- Preservados corte, áudio, PiP, efeitos, projeto JSON schemaVersion 4 e o comportamento das telas. Compilação Android e exportação em aparelho ainda pendentes.

## v0.1.39 (versionCode 40) · 21/10/2026
- Validação do exportador: duração intrínseca de mídia versus corte, validação URIs e renderização de overlays. Reproduzir em aparelho com o mesmo vídeo da captura; ainda pendente.

## v0.1.38 (versionCode 39) · 21/10/2026
- Executar `python3 tools/validate_source.py` e compilação Android + testes e Lint no GitHub Actions.
- Conferir Salvar da tela Legendas, carrossel circular do diálogo e recolhimento/expansão de volumes.
- Testar exclusão de faixa com confirmação e cancelamento, sem apagar a mídia.
- Validar visual em tela estreita e projeto com muitas legendas/faixas.

# Verificação v0.1.36 · 20/10/2026
- Diagnóstico dos logs do workflow 31: `MainActivity.kt:2926:21 Unresolved reference searchQuery` em Debug/Release. Corrigida a ordem da variável `searchQuery` antes do callback de importação SRT.
- Executar `python3 tools/validate_source.py` para validar metadados da v0.1.36/versionCode 37 e ordem de estado/importação. Essa verificação NÃO substitui o compilador Kotlin/Android.
- Pendente: executar `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` em GitHub Actions, conferir os cinco APKs e testar busca, importação SRT e vídeo/legendas no celular.

# Verificação v0.1.35 · 20/10/2026
- Código-fonte: `python3 tools/validate_source.py`, teste `AdaptiveVideoLayoutTest`, checagem sintática do `MainActivity.kt`, integridade ZIP e versionamento.
- Reprodução Android: verificar proporções 9:16, 16:9, 1:1, vídeo rotacionado 90/270 graus; mudar orientação de clipe durante a reprodução no editor, PiP e Exportar.
- Legendas: texto em retrato deve quebrar apenas entre palavras quando couber, posição arrastável e tamanho originalmente salvo preservados. Verificar palavra longa e legenda com fundo, sombra, contorno e alinhamento.
- Exportar MP4 e comparar com prévia. O ajuste de fonte da prévia é visual, não altera `sizeSp` nem o exportador; correspondência exata entre os dois formatos requer inspeção no dispositivo.
- Build Android, lint e testes no aparelho continuam pendentes nesta entrega.

# Verificação v0.1.34 · 20/10/2026
- Conferir `tools/validate_source.py`, sintaxe Kotlin da UI e helper e execução isolada do helper com `kotlinc`; executar testes JUnit em CI para validação final.
- Legendas com acentos: buscar `sao` localiza `São` e busca sem resultados é sinalizada. Após filtrar, a lista mostra os números originais. Limpar e fechar busca devem restaurar todas.
- Navegação anterior/próxima: o player posiciona no início da legenda correta, sem pular circularmente. Botões desabilitam nas extremidades.
- Excluir uma legenda: Cancelar preserva dados; Confirmar remove somente o item e seleciona um remanescente, sem modificar mídia original.
- SRT até 250 mil caracteres deve ser aceito quando válido; conteúdo com caractere extra deve ser recusado por inteiro, sem importação parcial.
- Nova legenda próxima ao último milissegundo do vídeo precisa manter início < fim <= duração; com vídeo vazio deve permanecer desabilitada.
- Pendente: Gradle/Android CI, fidelidade da prévia/exportação, interação real com IME, telas pequenas e aparelhos de desempenho limitado.

# Verificação v0.1.33 · 20/10/2026
- Confirmar `python3 tools/validate_source.py`, sintaxe PSI Kotlin da UI e do helper, testes Kotlin de janelas temporais, integridade CRC do ZIP.
- Legendas: prévia Media3 fixa ao rolar 30/300 itens, posição x/y por arrasto, player busca legenda selecionada e para no fim do trecho. Garantir liberação de ExoPlayer ao sair da tela.
- Validar seleção de legenda na timeline única (horizontal), rolagem automática da lista, criação e importação SRT, exclusão, ordem por tempo absoluto, sobreposição, campos início/fim e gesto de arrasto da legenda ativa.
- Testar no Redmi com projeto 90s/30 legendas, lista longa e legendas sobrepostas; confirmar tema claro/escuro, dimensões pequenas e comparação prévia/exportação MP4.
- Pendente: execução Gradle testDebugUnitTest/lintDebug/assembleRelease, testes reais Android e publicação dos 5 APKs. Não declarar APK compilado nesta entrega.

# Verificação v0.1.32 · 20/10/2026
- Validar `tools/validate_source.py`, parser Kotlin em arquivos alterados, testes de `TrimTimelineEditTest` e CRC do ZIP.
- Casos esperados: dividir em 4s e 10s, selecionar a parte da direita; excluir apenas 4–10s; remarcar corte no mesmo vídeo; reabrir projeto e conferir cursor 10s; desfazer remoção e marcador.
- A faixa de trechos usa `LazyRow` e carrega somente miniaturas dos itens visíveis. Verificar comportamento com clipes longos, tela estreita, rotação, temas claro/AMOLED e permissão persistente do URI.
- Metadados opcionais (`lastTrimCutClipId`, `lastTrimCutSourceMs`) não alteram schemaVersion 4; testar leitura de projetos antigos e transições pré-existentes.
- A compilação Android completa, lint, funcionamento real do ExoPlayer e exportação ainda precisam ser verificados em CI e no celular.

# Verificação v0.1.31 · 20/10/2026
- Executar `python3 tools/validate_source.py` para garantir versões, links de callbacks, lógica de corte, ações Save e configuração de APKs.
- Nova lógica `TrimSaveLogic.applyPending`: não permite clipe inexistente/trecho com menos de 250 ms, salva intervalo válido mantendo os demais clipes, não altera arquivo original. Três testes JUnit adicionados.
- Em aparelho, conferir barra inferior com rolagem circular nos dois sentidos, acesso a todos os cartões e toque no Salvar mesmo em tela pequena.
- Em Cortar e unir: verificar rolagem até Excluir/Salvar; salvar corte alterado e não aplicado; testar corte inválido bloqueado; voltar para Home e reabrir projeto comprovando persistência.
- Ainda sem compilação Kotlin/Android integral, verificação do lint, instalação de APK, teste de persistência Android e teste visual físico nesta entrega.

## v0.1.22
Pré-escuta adicionada com ExoPlayer e limitação de trecho. Requer compilação CI e teste em aparelho.

Correção v0.1.22: o workflow 19 compilou Kotlin Debug/Release, executou testes e lint com sucesso, mas falhou no empacotamento porque tools/package_apks.py ainda esperava 0.1.17/versionCode 17. Constantes sincronizadas para 0.1.22/versionCode 23. Compilação e publicação da nova versão pendentes.

# Uai Cortes 0.1.22 | Validação

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

## Build 16 / implementação v0.1.22 (versionCode 23)
- Falha confirmada no build 16: task `:app:lintDebug`, 3 erros Media3 `UnsafeOptInUsageError` em `MainActivity.kt`, first failure line 1530. Kotlin e etapas anteriores concluídos, mas o release **não** foi publicado.
- Corrigida a origem das APIs instáveis: helper `previewPlayer` anotado com `androidx.annotation.OptIn` fora das lambdas `AndroidView`.
- `app/build.gradle.kts`: saídas para arm32/arm64/x86/x86_64/universal com mesmo versionCode 23.
- `tools/package_apks.py`: usa `output-metadata.json` e aborta se faltarem ABIs, houver duplicatas, falhar `apksigner verify` ou houver divergência de pacote/versão/SDK.
- Removidos do workflow `push.tags: v*` e `actions/upload-artifact` para evitar duas compilações/duas publicações do mesmo APK. A Release é o único destino.
- **Testes locais:** validador estrutural e sintaxe Kotlin (parser PSI) quando disponíveis; sem Gradle/SDK Android local.
- **Próximos testes em CI:** `testDebugUnitTest`, `lintDebug`, `assembleRelease`, verificação 5/5 APKs e assets de Release. Depois testar instalação e reprodução no ARM32, ARM64, x86, x86_64 e universal, sem presumir que haverá todos os aparelhos físicos.

## Validação v0.1.23 (versionCode 24)
- Validar `tools/validate_source.py` e executar testes de `AudioTrackTimingTest` para limites de origem e projeto.
- Novo teste `ProjectStoreTest.audioTrackSourceOffsetRoundTripAndLegacy` para persistência e migração JSON.
- Ainda pendente: Gradle/Media3 Android, testes com arquivo M4A/MP4 e reprodução real, especialmente ao deslocar uma faixa e exportar MP4.

## v0.1.24 (versionCode 25)
- Executar `python3 tools/validate_source.py` e testes `AudioTimelineEditTest` no CI; verificar que metadados e 5 APKs são 0.1.24 / versionCode 25.
- Testar em aparelho: arrastar faixa no editor e soltar (persistência após reabrir), tocar faixa para editar, arrastar alça esquerda/centro/direita e cancelar/salvar, trechos de 250ms, bordas do projeto, áudio sem metadados de duração e exportar comparando com prévia.
- Sem build Gradle Android local nem teste físico nesta preparação.

## v0.1.25 (versionCode 26)
- Validação local: executar `python3 tools/validate_source.py` e verificar `TimelinePrecisionTest` (posição de cursor, limites de borda, clipagem de coordenadas e seleção de quadro por instante).
- Pendente no GitHub Actions: `testDebugUnitTest`, `lintDebug`, `assembleRelease` e verificação dos cinco APKs 0.1.25/versionCode 26.
- Pendente no Android físico: editor com vídeos 16:9/9:16, posicionar legenda por arrasto na imagem, reabrir projeto, comparar com MP4 exportado, testar vídeo com quadros indisponíveis e legendas compridas.
- Estática/Kotlin não substituem compilação Android nem comprovam fidelidade final das coordenadas.

## v0.1.26 (versionCode 27)
- Testar `SubtitleTimelineEditTest` com limites, arrasto e duração de legendas SRT curtas; manter validação estrutural de workflow/versões e PSI Kotlin.
- No Android: tocar em legenda específica na timeline principal; arrastar início/centro/fim na tela Legendas; confirmar que valores são gravados apenas ao soltar e sobrevivem à reabertura; verificar múltiplas legendas sobrepostas, sem espaço ou com tempo <100 ms.
- Pendente: Gradle `testDebugUnitTest`, `lintDebug`, `assembleRelease` e publicação dos cinco APKs versão 0.1.26 / versionCode 27.

## v0.1.27 (versionCode 28)
- Rodar `tools/validate_source.py` e testes JUnit `SubtitleStylesTest`/`ProjectStoreTest` no GitHub Actions. Conferir cinco APKs e metadados de publicação da versão 0.1.27.
- Confirmar no Android: cinco estilos, fundo transparente/claro/escuro, negrito, textos longos, vídeos 16:9 e 9:16, tema claro/AMOLED, cor/texto visíveis e resultado no MP4.
- Abrir projeto antigo sem `backgroundColor`/`bold`, verificar aspecto clássico; salvar/reabrir projeto editado; copiar estilo para todas, cancelar confirmação e confirmar preservação de horários, posição e texto.
- Análise sintática/estrutural local não equivale a build Gradle Android nem prova resultado final do compositor Media3.

## v0.1.28 (versionCode 29)
- Rodar validador estrutural, `SubtitleTypographyTest`, `SubtitleStylesTest`, `ProjectStoreTest` e testes Android Debug no GitHub Actions.
- Validar cinco APKs Release v0.1.28/versionCode 29 no metadata; SDK permanece API 36.
- Testar três fontes, contraste com fundo, contorno em 0/1/4 dp e escolha preto/branco em clipes 16:9 e 9:16. Comparar prévia vs MP4 exportado em texto longo, acentos e múltiplas linhas.
- Verificar abertura de projetos antigos sem campos de fonte/contorno e a ação de copiar o estilo para todas mantendo IDs, posição e tempo.
- A validação estrutural/Kotlin não substitui Gradle nem prova fidelidade de renderização do Media3.

## v0.1.29 (versionCode 30)
- Validador estrutural (`tools/validate_source.py`), testes `SubtitlePresentationTest` e regressões das versões anteriores no CI.
- Testar sombras pretas/brancas em 0, 1 e 6 dp; espaçamento 100/160% e alinhamento esquerda/centro/direita em textos longos/multilinha e com acentos.
- Reabrir projeto antigo sem campos novos; copiar aparência de uma legenda a outra e verificar preservação de texto/horário/posição.
- Pendente: `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleRelease`, cinco APKs e comparação quadro a quadro de prévia versus MP4 em 16:9 e 9:16.
- Não considerar equivalência de rendering provada apenas por análise de sintaxe ou testes JVM de lógica.

## v0.1.30 (versionCode 31) · correção do workflow 26
- O workflow 26 da v0.1.29 falhou com `Unresolved reference 'PathJoin'` no import e no traço da prévia (MainActivity.kt). A execução atingiu o compilador Kotlin de Debug e Release; não houve APK final.
- Substituir o import inválido por `androidx.compose.ui.graphics.StrokeJoin` e usar `StrokeJoin.Round` em `Stroke(...)`.
- Rodar `python3 tools/validate_source.py`, conferir que não existe `PathJoin` em `MainActivity.kt`, validar `tools/package_apks.py`, metadados Gradle/workflow e testar a integridade do ZIP.
- **Pendente em CI**: `gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`, empacotamento e publicação dos cinco APKs da v0.1.30.
- **Pendente no Android real**: checagem de contornos e alinhamento das legendas na prévia e no MP4 final. A correção resolve a referência de tipo indicada no log, não comprova ausência de outros erros.

### Diagnóstico local (v0.1.42)
- [x] Validador estrutural identifica a rota, ativação opcional, observador de toques, integrações de erro, FileProvider e nenhum envio automático.
- [x] Kotlin PSI validou a sintaxe dos arquivos; casos locais de sanitização, classificação Media3 e coordenadas em grade passaram.
- [ ] Compilar Android Release/Debug e executar testes unitários oficiais com Gradle no GitHub Actions.
- [ ] Verificar no Android físico toques em todas as telas, diálogos/sheets, mudança de rota, rotação e reinício após crash. Toques de janelas externas do sistema não são registrados.
- [ ] Reproduzir exportação com erro Media3 e compartilhar manualmente TXT para conferir dados, privacidade e detalhes de diagnóstico.
