# Regras de manutenção do Uai Cortes
- Atualizar sequencialmente `versionName`, `versionCode`, README, RELEASE_NOTES e nomes do APK em workflow.
- Entregar cinco APKs Release diretamente em Release Assets no formato `UaiCortes-vX.Y.Z-{arm32,arm64,x86,x86_64,universal}.apk`; nunca publicar APK duplicado como Actions Artifact.
- Entregas de código-fonte podem ser ZIP, mas a saída de instalação não pode ser ZIP.
- Sem autenticação, publicidade ou serviço de nuvem para edição. Reconhecimento de fala por IA local é opcional e requer download voluntário do modelo; nunca transmite áudio do usuário.
- Não declarar recurso pronto sem implementação e teste. Dissolvência real continua pendente; fade para preto/branco precisa de compilação e teste em aparelho.
- Nunca adicionar chaves, tokens ou keystores ao repositório.

- PiP: manter aspecto original ao carregar logotipos, sem crop, controles Começa em/Termina em e nove atalhos de posição, agora com quatro controles quadrados compactos em uma linha e painel inferior ModalBottomSheet por cima dos controles (nunca no centro ou expandindo a página).
- PiP: manter lista de até quatro camadas e reposicionamento automático por espaço e intervalo de tempo; evitar alterações silenciosas se não couber.
- PiP: manter biblioteca por imagens/vídeos e pastas, alternativa SAF, controle de áudio de vídeo separado e opção silenciosa persistida.
- Confirmar fidelidade entre visualização e exportação no aparelho físico antes de afirmar funcionamento completo.

- PiP v0.1.10: Bottom Sheet sem alça superior, 4 ferramentas na mesma linha, edição direta dos horários, PiPs sequenciais (começar depois do último) e diálogo quando acabar o vídeo.
- PiP v0.1.10: colisões detectadas por região e período; permitir sobrepor somente mediante aprovação explícita do usuário para o par afetado, persistindo `overlapWith` no JSON, preservando auto-reposicionamento como alternativa.

- v0.1.11: tela Cortar e unir com preview do vídeo, miniaturas locais, RangeSlider duplo, cursor de reprodução, marcação início/fim, aplicação explícita, opção desfazer, divisão, reordenar e excluir.
- v0.1.11: entrada de tempo compartilhada `NumericTime` usa dígitos em décimos da direita para esquerda; máscara sem separadores no valor, validação do intervalo antes de salvar; reutilizar nos horários da tela PiP.

- v0.1.12: Cortar e unir deve manter miniaturas tocáveis abaixo do player, sem RangeSlider sobre elas; campo Tempo `autoApply` move o cursor imediatamente após entrada válida e os campos início/fim continuam locais até Aplicar corte.
- v0.1.12: botões Dividir, Desfazer, Antes, Depois e Excluir em uma única linha; marque fim deve explicar quando o cursor não produz intervalo mínimo, e Desfazer cobre a última alteração de lista de clipes.

- v0.1.13: reproduzir indefinidamente na prévia de corte (REPEAT_MODE_ONE) e editor (REPEAT_MODE_ALL), até pausar.
- v0.1.13: seletor 0,25×–2× só afeta prévia; sincronizar PiPs com velocidade do editor. Nunca afetar o MP4 exportado sem escolha separada do usuário.
- v0.1.13: mini tutorial textual abaixo dos botões Cortar/Dividir/Desfazer/Mover/Excluir, preservando organização compacta e suporte a temas.

- v0.1.14: mesma geometria e barras retangulares nas faixas Vídeo/PiP/Áudio/Legenda; toque em clipe abre Bottom Sheet com volume individual, duplicar, cortar, mover e excluir com confirmação.
- v0.1.14: schemaVersion 4, legendas manuais e SRT por tempo absoluto, fades por par de IDs, faixas de áudio extras, extrair M4A/AAC com SAF, sem inventar suporte a MP3/WAV ou dissolvência.
- Conservar compatibilidade de projetos legados, controle de volume na prévia/exportação, 4 PiPs e progressão sequencial.

- v0.1.15: a API `ModalBottomSheet`/`rememberModalBottomSheetState` exige opt-in Kotlin Material3 nos respectivos composables; preservar o opt-in AndroidX do Media3 separadamente. Capturar `maxWidth` diretamente no `BoxWithConstraints` antes de entrar em `Row` aninhado. Não confundir análise sintática com build Android.

- v0.1.16: manter `splits.abi` nas quatro ABIs e `isUniversalApk=true`, todas com o mesmo versionCode para downloads externos (não configura multi-APK Play Store). Empacotar somente metadados Release via `tools/package_apks.py`; abortar CI se não houver exatamente cinco saídas válidas.
- v0.1.16: não adicionar gatilho `push.tags: v*` ao workflow de Releases automáticas; criação de tag pelo `gh release create` provoca segundo build. Não adicionar `actions/upload-artifact` com cópia dos APKs já publicados.
- v0.1.16: chamadas `UnstableApi` da Media3 devem permanecer isoladas em função Kotlin anotada com `androidx.annotation.OptIn`, não dentro de lambdas `AndroidView` de Compose; validar lint no CI.

- v0.1.23: manter `AudioTrack.sourceStartMs` (entrada no arquivo) distinto de `startMs` (posição no projeto); `durationMs` é a duração do trecho após o recorte. Projetos antigos assumem início de fonte 0 ms.
- Ajustes de faixa precisam refletir na prévia do editor e no `ExportEngine`, sem modificar arquivos originais. Não afirmar teste de áudio real sem Android.

- v0.1.24: edição visual de áudio deve modificar campos `sourceStartMs`, `startMs`, `durationMs` apenas por ações explícitas; alças de trim no editor de Áudio são rascunho até Salvar corte, enquanto o arrasto da timeline principal grava a posição ao soltar.
- Gestos não podem mover/clipe para fora do projeto ou da mídia de origem, nem produzir trecho <250 ms. A faixa de música legada continua abrindo a tela Áudio, sem fingir recorte suportado.

- v0.1.25: legendas usam posição normalizada em x/y, com âncora horizontal central e vertical inferior igual ao exportador. Não restaurar offsets fixos em dp; verificar formatos de vídeo distintos antes de afirmar fidelidade completa.
- O arrasto da legenda só persiste no projeto ao soltar; a miniatura é recurso local opcional. Passos de ajuste fino do cursor mantêm limites [0,duração].

- v0.1.26: edição de legendas mantém IDs e intervalos absolutos; cada bloco da timeline deve selecionar o texto correto. Gestos modificam só `startMs`/`endMs`, salvando ao soltar, sem alterar o arquivo SRT ou reescrever propriedades visuais. Não bloquear legendas legadas curtas; manter campos numéricos de tempo como alternativa.
- v0.1.27: `SubtitleCue.backgroundColor` e `bold` devem manter defaults compatíveis com projetos legados; estilos são aplicados à prévia e à exportação, com cor ARGB. Fundo transparente não recebe BackgroundColorSpan. Copiar aparência para todas nunca modifica IDs, texto, início/fim ou posição X/Y. Não aumentar schemaVersion sem necessidade de migração.

- v0.1.28: guardar apenas IDs de famílias de fontes locais (`sans`, `serif`, `mono`) em `SubtitleCue` e normalizar desconhecidas para `sans`; contorno limitado a 0–4 dp (0 = desabilitado). Projetos antigos assumem fonte sans-serif e nenhum contorno. Não mexer na posição ou tempos ao aplicar estilos e não alterar schemaVersion 4. Conferir fidelidade de fontes/contornos e alinhamento multilinha no MP4 real antes de declarar equivalência completa.

- v0.1.29: `SubtitleCue.shadowOffsetDp` (0–6), `shadowColor` (preto/branco), `lineSpacingPercent` (100–160) e `textAlignment` (`left`/`center`/`right`) são atributos opcionais por legenda; defaults legado 0, preto, 100, centro. Contorno/fonte e schemaVersion 4 preservados.
- Sombra implementada como cópia de texto deslocada, sem blur; espaçamento e alinhamento afetam linhas dentro do bloco, não posição X/Y. Conferir Compose versus Media3 no Android antes de afirmar fidelidade visual total.

- v0.1.30: no Compose, o parâmetro `Stroke.join` usa `androidx.compose.ui.graphics.StrokeJoin`; `PathJoin` não existe nesta API. Não reintroduzir o import ou uso antigo. O workflow 26 falhou em Debug/Release por esse motivo. Atualizar sempre Gradle, workflow, `tools/package_apks.py`, validador e docs ao avançar de versão.

- v0.1.31: o cartão Salvar no editor chama a persistência de projeto antes de voltar à Home, e a barra inferior é rolável circularmente nos dois sentidos. Não confundir salvar projeto com exportar MP4.
- v0.1.31: na tela Cortar e unir, o novo Salvar permanece após Excluir em faixa horizontal; deve validar e aplicar o corte pendente antes de voltar à Home, mantendo o usuário na tela se o intervalo for inválido.

- v0.1.32: ao dividir, criar parte esquerda e direita na ordem original, selecionar a direita e posicionar o cursor na fronteira usando horário de origem. Persistir IDs/horários opcionais do último corte sem mudar `schemaVersion=4`.
- v0.1.32: faixa LazyRow mostra miniatura de cada parte visível (carregamento sob demanda), duração e seleção; rolar automaticamente para o trecho escolhido. Desfazer e excluir devem manter marcador consistente, sem afetar arquivos originais nem transições de clipes vizinhos.

- v0.1.33: Legendas tem uma única timeline horizontal virtualizada e lista LazyColumn compacta. Não reintroduzir uma pista completa por legenda nem duplicar lista. Prévia Media3 fica acima da lista, não acompanha rolagem; apenas o editor da legenda ativa pode expandir.
- v0.1.33: edição avançada inicia recolhida; preservar campos numéricos de tempo, gestos para ajustar intervalo, arrasto x/y, presets/fonte/contorno/sombra e `schemaVersion=4`. Ao escolher uma legenda, reproduzir prévia a partir do início; prévia não altera exportação. Validar alinhamento UI/export em aparelho.

- v0.1.34: filtro textual de legendas deve ser insensível a acentos/maiúsculas, manter a numeração original e não alterar projeto enquanto se busca. Navegação cronológica anterior/próxima não deve fazer loop entre extremos; a prévia deve buscar o início da legenda ativa.
- v0.1.34: exclusão requer confirmação explícita e não pode remover o arquivo de vídeo. Criar legenda apenas se houver duração positiva, com horários dentro de [0, duração]. SRT maior que 250 mil caracteres deve falhar visivelmente, sem importar conteúdo truncado. Manter `schemaVersion=4`.

- v0.1.35: toda prévia de vídeo deve preservar a proporção do arquivo e, em projetos mistos, acompanhar o clipe ativo. Prévia inline usa altura responsiva limitada pela tela, preservando lista/controles abaixo.
- v0.1.35: legenda na prévia adapta tamanho apenas quando a maior palavra não cabe na largura do vídeo; não modificar `SubtitleCue.sizeSp`, posição, tempos, SRT ou exportador de forma implícita. Confirmar alinhamento visual e MP4 com teste físico antes de declarar fidelidade total.

- v0.1.36: `searchQuery` deve ser declarado antes de `rememberLauncherForActivityResult` na tela `SubtitleScreen`, pois o callback de SRT limpa a pesquisa; o workflow 31 falhou em Debug/Release quando a variável estava declarada depois. Preservar teste estrutural de ordem no validador, mas sempre confirmar build Android no CI.

- v0.1.37: `SubtitleCue.sizeSp` padrão de novas legendas = 12; ao ler projeto anterior sem `sizeSp`, assumir 22. `animation` e `animationSpeed` opcionais com defaults sem efeito / 1.0. Não elevar schemaVersion=4.
- v0.1.37: editor modal usa rascunho; Cancelar não persiste edições. Aplicar a todas sempre confirma e copia somente aparência, posição e/ou animação conforme seleção; IDs, texto e tempo ficam intactos. Preview e Media3 usam o mesmo SubtitleAnimations.frame, mas fidelidade de renderização física depende de teste Android.

- v0.1.38: Salvar em Legendas persiste projeto editável e volta à Home; preservar Nova/SRT e busca na mesma área. Efeitos devem ficar em LazyRow circular sem duplicar dados persistidos.
- v0.1.38: Áudio mantém volumes legados acessíveis por expansão e faixas adicionais acessíveis sem expandir, com volume individual opcional; remover faixa precisa de confirmação e não apaga arquivo original. Projetos antigos continuam sem migração (`schemaVersion=4`).

- v0.1.39: `EditedMediaItem.Builder.setDurationUs` para vídeo/áudio NÃO deve receber a duração do recorte. Para `Transformer` com duração intrínseca, deixe Media3 ler o arquivo original. Mantenha clipping explícito e valide acesso às mídias e os limites dos cortes.
- v0.1.39: prévia de Exportar desenha legendas/PiPs/fades salvos, mas a exportação real precisa ser testada em aparelho com áudio e vídeo. Apresentar código/causa original em erros de AssetLoader, não afirmar que foi resolvido sem novo teste.

- v0.1.40: `TextOverlay.getBitmap` não aceita texto vazio, pois gera StaticLayout de largura zero. `TimedSubtitleOverlay.getText` deve sempre fornecer placeholder NBSP transparente nos intervalos sem legenda ou na primeira amostra do typewriter, e `getOverlaySettings` precisa usar alpha 0 nessas condições. Guardar texto/tempos/SRT originais sem alterações; testar exportação física antes de declarar resolução definitiva.

- v0.1.41: diagnóstico desativado por padrão; registros locais de tela/toque aproximado (5%) e erros categorizados, sem texto/URI/nome de mídia/projeto/tokens. Histórico rotativo ≤512 KiB, apagável; compartilhamento apenas iniciado pelo usuário. Não gravar teclas ou capturar tela e não adicionar upload automático.
- v0.1.41: usar FileProvider limitado ao cache de diagnósticos, respeitar callbacks de erros Media3/transformer e manter compatibilidade de projetos (`schemaVersion=4`). Confirmar taps, crashes, exportação e permissões no aparelho após compilar.

- v0.1.42: legenda na exportação deve fazer StaticLayout no máximo 90% da largura REAL do quadro, com fonte escalada pela largura e quebra automática. Não regressar ao TextOverlay padrão de largura baseada em linha única. Teste em MP4 físico pendente.
- v0.1.42: mostrar confirmação de sucesso em tela cheia SOMENTE depois da gravação concluída no MediaStore. Deve oferecer abrir, compartilhar, fechar e voltar ao editor; não exibir sucesso se falhar.

- v0.1.43: antes da confirmação em tela cheia da exportação, declarar `savedName` e `savedVideo: Uri?` como estado de `ExportScreen` ANTES de callbacks `onSuccess` do Media3. CI workflow 37 falhou por referências não resolvidas; regressão estrutural no validador não substitui compilação real.

- v0.1.44: todas as importações visuais (novo projeto, adicionar clipes, PiP e vídeo para extração de áudio) passam pela galeria interna MediaStore. Usar SAF como alternativa, respeitar acesso parcial Android 14+, não supor URI MediaStore persistente após revogação. A importação múltipla de clipes preserva ordem e não copia arquivos originais; manter schemaVersion 4.

- v0.1.45: ao começar a exportar, abrir uma tela cheia bloqueada contra retorno acidental, com percentual Media3 apenas quando `PROGRESS_STATE_AVAILABLE`, cronômetro e confirmação de cancelamento.
- v0.1.45: após `Transformer.onCompleted`, mostrar etapa de cópia MediaStore indeterminada, não mostrar conclusão antes do sucesso efetivo. Cancelamento desativado durante a cópia; não alegar exportação em segundo plano ou ETA precisa.

- v0.1.46: transições são rascunho até o botão inferior Aplicar; atualizar apenas a junção escolhida ou todas as junções adjacentes se solicitado. O botão retorna ao editor após atualizar o projeto; voltar sem aplicar não grava mudanças.
- v0.1.46: modos adicionais Flash branco/preto, Fade azul/dourado, Preto e branco e Sépia atuam por matriz RgbMatrix em cada quadro, sem crossfade ou sobreposição entre clipes. `TransitionEffects` é lógica pura compartilhada com a prévia de quadros. Preservar fades legados e `schemaVersion=4`, validar MP4 real antes de afirmar identidade visual completa.

- v0.1.47: LaunchedEffect da galeria nunca transforma CancellationException / LeftCompositionCancellationException em erro. Cancelamento é propagado, indicadores não são atualizados por consultas abandonadas, e erros reais oferecem retry/SAF sem exibir caminhos. O diagnóstico de permissionamento em player do projeto é independente.

- v0.1.48: marca d'água em texto é propriedade opcional `VideoProject.textWatermark`, não legenda comum. Manter projeto JSON schemaVersion 4, presets de 9 posições, transparência e texto até 80 caracteres; remover marca não exclui legendas.
- Prévia do editor e de Exportar deve renderizar marca independentemente das legendas. Media3 deve adicionar `TimedSubtitleOverlay` exclusivo da marca em cada clipe, além do overlay de legendas existente. Validar MP4 real em aparelhos com orientação vertical/horizontal.
- Não anunciar legendas automáticas sem transcrição real: backend local deve ser opcional com download de modelo, limites de memória e revisão de SRT antes de aplicar; nenhuma API externa ou envio de áudio sem consentimento explícito.

- v0.1.49: voz automática precisa ser transcrita de verdade do áudio existente, respeitando cortes por clipe e 16 kHz mono PCM. Vosk opt-in; download HTTPS oficial e ZIP validado; nunca enviar áudio à nuvem. Permitir revisar e corrigir antes de gravar. Limite inicial de 10 minutos; não afirmar transcrição física validada sem APK em celular.
- v0.1.49: projeto deve solicitar confirmação antes de excluir e não apagar mídias originais; miniaturas são extraídas sob demanda pelo MediaMetadataRetriever na thread IO e devem falhar silenciosamente caso a permissão tenha sido revogada.
- v0.1.49: transições de zoom/pan devem ser realmente exportadas via MatrixTransformation, com identidade fora das janelas; não rotular como crossfade (não suportado pela versão atual Media3).

- v0.1.50: preservar módulo Animar Imagens totalmente offline, padrão 9:16/720p/30 FPS/5 s, seleção e reordenação, prévia, exportação H.264 e integração opcional na timeline. Limite 80 imagens; nenhum download nem modelos externos; paralaxe 2.5D não implementada. Conferir Media3 e desempenho em aparelho antes de alegar validação física. Atualizar versionCode e metadados sequencialmente.

- v0.1.51: nenhum `progress!!` em callbacks Compose, usar percentual local imutável limitado [0,1] ou indicador indeterminado. Regressão para `NullPointerException` de `LinearProgressIndicator`.
- v0.1.51: tela Animar Imagens mantém proporção com prévia compacta 238 dp, ordenação durante arrasto na alça e rolagem de borda, setas e remoção. Ajustes/Animar em mesma linha, mensagens de exportação com nome Uai Cortes.

- v0.1.52: Animar Imagens mantém rascunho local de URIs (MediaStore/SAF) e `ImageAnimationOptions`, sem alterar o schemaVersion 4 de projetos de vídeo. Não converter fotos em clipes MP4 fictícios; renderizar primeiro e depois usar a timeline existente para áudio/legendas/PiP/cortes.
- v0.1.52: usar `VideoSavedDialog` compartilhado somente após MediaStore concluir, botão Salvar no topo e confirmação ao sair com alterações não gravadas. Nunca bloquear Voltar após sucesso; durante gravação final impedir cancelamento sem explicação. O rascunho não garante acesso a mídias se a permissão Android tiver sido revogada.

- v0.1.53: `MainActivity.kt` coordena o aplicativo; telas extraídas por funcionalidade ficam em arquivos próprios no pacote `br.com.uaicortes`. Não voltar a concentrar telas nesse arquivo. `EditorWorkspaceState` é estado transitório, não substitui `ProjectStore` nem garante recuperação após encerramento do processo.
- v0.1.53: ExportEngine e ImageAnimationEngine devem salvar MP4 por `GalleryVideoSaver`; nunca confirmar sucesso antes do MediaStore finalizar. Em erro, remover URI parcial; gravação ocorre fora da thread de interface.
- v0.1.53: `ProjectStore` exige JSON legível ou backup válido antes de salvar/excluir; impedir substituição silenciosa por `[]` ao ler dados corrompidos. Preservar `schemaVersion=4`, projetos legados e histórico. Gradle/Android e testes em aparelho continuam obrigatórios antes de declarar versão validada.

- v0.1.54: `EditorWorkspaceState` deve ser criado com `rememberSaveable` e saver de identificadores pequenos. `activeProjectId` também é saveable; o projeto vem do `ProjectStore`, nunca do Bundle. Não incluir JSON, bitmaps, gravações, tarefas de exportação ou chaves no saved state.
- v0.1.54: navegação/Voltar centralizada em `EditorNavigation`. Ao recuperar estado, verificar existência de clipe/áudio/legenda/PiP e limites de tempo. Falta de projeto volta para Home, evitando telas vazias. Morte de processo sem savedInstanceState, tarefas de exportação e estados internos de ferramentas não têm retomada garantida; não alegar isso.

- v0.1.55: preservar a extração `ProjectController`/`ProjectRepository`, `OverlayOperations` e `MediaImportRules`/`MediaImportController`. A UI deve confirmar mudanças de projeto apenas após `ProjectStore.save/delete` concluídos; não permitir que falha de cadastro substitua projetos em memória.
- v0.1.55: edição de PiP continua limitada a quatro camadas; inserção em tempo sobreposto exige confirmação e colisões precisam manter os pares aprovados por ID. O seletor SAF mantém a tentativa de persistir permissão de leitura, sem presumir concessões da galeria.
- v0.1.55: `MediaImportRules` não deve inventar dados de fontes inválidas; o helper legado `mediaDuration` ainda possui fallback de 1000 ms e merece auditoria específica antes de alegar validade de mídia. `schemaVersion=4` permanece igual. Teste completo Android/CI e validação física são pendentes.

- v0.1.56: exportação de vídeo solicita 8 Mb/s em 720p e 18 Mb/s em 1080p via DefaultEncoderFactory e VideoEncoderSettings; áudio AAC 192 kb/s. Manter fallback do Media3; hardware pode ignorar parâmetros. Registrar bitrate efetivo sem URIs no log e não alegar resultado 1080p/18Mb sem teste real.
- v0.1.56: Animar Imagens prepara arquivos PNG temporários sem perdas para evitar JPEG intermediário; manter checagem de espaço livre, limpeza de arquivos de trabalho e rascunho legado. O padrão do editor para instalação nova é 1080p, preservando escolha já salva.

- v0.1.56: verificar espaço projetado para 2 arquivos MP4 mais reserva de 96 MiB antes de exportar e durante o preparo de PNG; não afirmar que essa estimativa garante conclusão.

- v0.1.57: índices de junção são zero-based; com 2 clipes existe 1 junção, índice 0. Ao remover clipes sem trocar ID do projeto, revalidar seleções em `EditorWorkspaceState` e usar `TransitionBoundaryPolicy` antes de acessar `p.clips[index + 1]`. O teste do workflow 50 esperava incorretamente índice 1.
- v0.1.57: metadados de vídeo da galeria/SAF são obtidos fora da thread UI; só aplicar o lote se o projeto ativo ainda for o original, sem sobrescrever outro projeto. Durações não verificáveis são rejeitadas na importação; manter fallback legado fora desse fluxo por compatibilidade.

- v0.1.58: `MainActivity` usa `rememberCoroutineScope`, `importScope.launch`, `withContext(Dispatchers.IO)` e captura `CancellationException` que deve ser sempre relançada; verificar imports correspondentes antes de submeter o workflow, além da compilação Android real. CI 51 falhou por importações ausentes, não por incompatibilidade do Media3.

- v0.1.59: `MediaImportTasks` deve consultar durações e nomes de vídeos/áudio/PiP apenas em dispatcher de IO. Atualizações do `VideoProject` e de estado Compose devem ocorrer no dispatcher principal.
- v0.1.59: não iniciar importações simultâneas; descartar resultados atrasados ao mudar projeto, tela ou ferramenta. Preservar cancelamento de corrotinas, permissões SAF e o `schemaVersion=4`.
- v0.1.59: o workflow 51 exigiu imports explícitos de corrotinas. O validador agora verifica a arquitetura com serviço compartilhado, mas a compilação Android real continua obrigatória.

- v0.1.60: `MediaTaskCoordinator` pertence ao ciclo de composição; não reter Activity/Context no serviço nem declarar recuperação de tarefas após rotação/morte do processo. `DisposableEffect` cancela ao sair da composição; a mudança de projeto/tela/ação cancela a importação pendente.
- v0.1.60: sempre usar `MediaTaskTicket` ao liberar a exclusão mútua. Uma tarefa antiga, mesmo do mesmo tipo, não pode liberar a nova. Propagar `CancellationException` sem reportar falha; nunca gravar projeto após importação cancelada.
- v0.1.60: testar no CI Gradle, no Android real e em rotação/troca rápida de tela antes de alegar estabilidade definitiva. O botão Cancelar atua na leitura/importação, não no processo de exportação MP4.

- v0.1.61: `EditorProjectViewModel` é dono do catálogo e do projeto ativo; não voltar a duplicar `projects`, `project` ou `activeProjectId` em `remember` da UI. O ViewModel usa `applicationContext` e `ProjectSessionCoordinator`, nunca `Activity`/`Composable`/`Uri` de tarefa.
- v0.1.61: no `SavedStateHandle` persistir somente o identificador `editor_active_project_id`; recuperar dados através de `ProjectStore`, sem serializar vídeo, mídia, JSON ou trabalhos de exportação. `EditorWorkspaceState` permanece saveable na composição para seleções de interface.
- v0.1.61: manter snapshots inalterados se operações do repositório lançarem exceção. Não criar projeto fictício ao restaurar um ID que não existe mais. Validar rotação e recuperação após morte de processo em testes Android reais antes de afirmar estabilidade completa.

- v0.1.62: estado do MP4 principal pertence a `ExportTaskViewModel`, não a `remember` do ExportScreen; não serializar `ExportEngine`/Uri/frames/arquivos no SavedStateHandle. Progresso desconhecido deve permanecer indeterminado.
- v0.1.62: manter tickets de `ExportTaskCoordinator` para ignorar callbacks atrasados; não cancelar o Transformer durante a cópia de arquivo final ao MediaStore; finalizar e limpar na thread principal. Testar rotação, cancelar, sair e falha de espaço no Android antes de afirmar fidelidade.

- v0.1.63: `ImageAnimationTaskViewModel` gerencia o motor e progresso do Animar Imagens, reutilizando `ExportTaskCoordinator` sem gravar URIs/bitmaps/jobs no SavedStateHandle. Não reintroduzir `ImageAnimationEngine` ou `Job` em remember da tela.
- v0.1.63: cada tentativa de animação usa motor independente; cancelamento durante preparo não remove arquivos em escrita nem afeta nova tentativa. Não cancelar a cópia final MediaStore; rejeitar callbacks de tickets antigos. Rotação pode preservar a tarefa se ViewModel sobreviver; morte de processo não retoma exportação.

- v0.1.64: `TimelinePlaybackCoordinator` é a fonte única da conversão entre tempo global e clipe, inclusive para ±5 s; não voltar a duplicar laços de seek na UI. Fronteira exata mantém o clipe anterior por compatibilidade com projetos antigos.
- v0.1.64: `EditorPlaybackViewModel` conserva somente ID de projeto e velocidade da prévia no SavedStateHandle. Nunca guardar ExoPlayer/MediaItem/Uri de tarefa; o player continua com release no DisposableEffect da tela.
- v0.1.64: durante scrubbing, não sobrescrever o cursor com polling; restaurar posição saveable após setMediaItems/prepare; comunicar isPlaying pelo Player.Listener. Preservar velocidades exclusivamente de prévia e schemaVersion 4. Validar em CI e aparelho real antes de alegar estabilidade.

- v0.1.65: `ProjectCollectionCodec.inspect()` deve distinguir instalação nova, primário válido, recuperação por backup e corrupção de ambas as cópias. Nunca tratar uma chave primária ausente como `[]` quando existir backup. `prepareWrite()` só permite backup de um JSON validado, nunca texto corrompido. IDs duplicados/vazios são inválidos. Falha de gravação não pode navegar para Home como se tivesse sido salva; UI deve mostrar aviso e manter o estado atual. Preservar `schemaVersion=4`, compilar e testar em Android real antes de declarar estabilidade.

- v0.1.66: `ImageAnimationDraftCodec` deve manter compatibilidade com JSON antigo sem `fps`; nunca tratar rascunho corrompido como lista vazia e sobrescrever sem confirmação. Preservar somente o JSON anterior validado em `backup`, incluindo recuperação quando a chave principal está ausente.
- v0.1.66: verificar acesso real às URIs antes de renderizar, fora da thread UI; não apagar imagens de rascunhos automaticamente quando a permissão for revogada. SAF `OpenMultipleDocuments` mantém tentativa de concessão persistente; MediaStore depende das permissões do Android e deve ser revalidado ao reabrir. `schemaVersion=4` e qualidade de exportação inalterados.

- v0.1.67: manter nome do projeto separado de nome MP4, sanitizar com ProjectNaming e não alterar `schemaVersion=4`. Excluir tudo pede confirmação e gravação atômica; não excluir mídias originais. Galeria de tipo único sem chip redundante e pesquisa opcional. Instalador Vosk fica em Configurações, com progresso percentual real/indeterminado e erro explícito; não afirmar download testado sem execução em rede real. PiP usa faixas globais derivadas dos cortes e nunca alonga vídeo PiP além da duração da fonte sem loop implementado.

- v0.1.68: manter uma única fonte `AppReleaseNotes` para Sobre/Novidades; assinatura do conteúdo de novidades evita repetição em compilações sem alterações. Registrar como vista somente ao continuar/voltar; permitir acesso manual. Não alterar schemaVersion 4, projetos, mídia ou exportação.

- v0.1.69: suporte à instalação de Vosk de ZIP via SAF após baixar do endereço oficial no navegador. Usar mesmo validador de ZIP das transferências internas, limitar bytes compactados/extraídos e rejeitar path traversal. Nunca instalar sem os arquivos essenciais exigidos pelo layout detectado (legado ou moderno). Preservar reconhecimento offline e não enviar áudio. Progresso deve apresentar bytes reais mesmo em 0% ou tamanho desconhecido; não afirmar rede validada sem testes.

- v0.1.70: `vosk-model-small-pt-0.3` oficial usa layout legado plano (`final.mdl`, `mfcc.conf`, `HCLr.fst`, `Gr.fst`), que o Vosk nativo suporta. Não voltar a exigir exclusivamente `am/final.mdl` e `conf/model.conf`; continuar aceitando também o layout moderno. Verificar integridade e caminhos seguros no ZIP. Três botões de instalação devem permanecer lado a lado em uma linha e funcionar em largura pequena.

- v0.1.71: não exibir `stage` antiga se `installed=true`; limpar `stage` quando importação/download confirmam instalação, manter botão tríplice, ZIP legado/moderno e testes de status. Detecção de arquivos não equivale a reconhecimento de fala testado.

- v0.1.72: velocidade da prévia de 0,25× a 5× com opções compactas; nunca aplicar velocidade ao MP4 sem opção separada. Um botão câmera ao lado da velocidade salva quadro original em PNG usando o tempo global, o início de cada corte e `MediaMetadataRetriever.OPTION_CLOSEST`. Não confundir captura do arquivo com screenshot de PiP/legendas da composição; informar a diferença.
- v0.1.72: salvar em `Pictures/UaiCortes` com MediaStore `IS_PENDING` Android 10+, descartar entradas parciais em erro, recuperar falhas de permissão sem crash. Android 8/9 usa pasta específica do app. Analisar orientação real, codecs, cortes e velocidades 3×–5× no APK antes de confirmar fidelidade.

- v0.1.73: velocidade da prévia com -/+/toque no valor, 14 passos 0,1×–6×, restaurar 1×; não alterar MP4. Substituir ajustes finos por buscas aproximadas de quadro via FPS nominal quando disponível; não prometer frame stepping exato em arquivos de FPS variável. Manter captura PNG. Verificar Media3 no aparelho antes de declarar velocidades extremas validadas.

- v0.1.74: preservar organização da tela Áudio em três cartões com UI em `AudioSectionUi.kt`. Não substituir seletor SAF por URI efêmera sem persistência; áudio escolhido já entra no projeto. Manter controles de volume por clipe e trilha, corte com rascunho, confirmação de exclusão, pré-escuta e extração M4A/MP4. `schemaVersion=4` inalterado.

- v0.1.75: faixa de áudio com `clipId` e `clipSourceAnchorMs` opcionais mantém JSON esquema 4. Resolver posição/recorte da faixa ancorada dinamicamente na prévia, timeline e exportação; após dividir/duplicar preservar fragmentos com novos IDs. Na remoção ou desfazer de divisão, limpar trilhas órfãs. O controle de volume no editor e Áudio do clipe nunca alteram a origem. Não alegar build Android sem CI.

- v0.1.76: Gemini TTS opcional é exceção explícita à edição offline: somente texto e parâmetros TTS enviados após ação do usuário. Não enviar vídeos nem chaves em diagnósticos.
- v0.1.76: credencial AES/GCM no Keystore e arquivo privado sem backup; importação SAF, cópia/exportação sob confirmação. Nunca embutir chaves. A versão pública com credencial administrada exige backend.
- v0.1.76: usar apenas modelos reais `gemini-3.8-flash-tts` e `gemini-3.8-flash-lite-tts` pela Interactions API; WAV unary com RIFF intacto. Inserir áudio como AudioTrack global com URI durável mantendo schemaVersion=4; compilar e validar renderização final em dispositivo antes de declarar equivalência.

- v0.1.77: Gemini aparece em Configurações como `ItemCard` navegável para `gemini_settings`. Back retorna a Áudio se originado em Áudio, senão a Configurações; não inserir o formulário diretamente na listagem.
- v0.1.77: Áudio mostra somente `Gerar por IA`; modal de narração usa rolagem interna, escolha de vozes pesquisável por nome/ID oficial sem inventar descrição ou código. Não descartar o rascunho de texto ao fechar durante a sessão, não fechar durante geração e preservar confirmação de cota.
- v0.1.77: preservar Keystore, SAF, API Interactions, WAV, schemaVersion 4, CI de cinco APKs e assinatura. Testar UX real/teclado/back e build Gradle antes de afirmar funcionalidade Android validada.

- v0.1.78: salvar na tela Legendas retorna ao editor do mesmo projeto somente após persistência bem-sucedida. Não usar Home no callback `onSaveEditor`.
- v0.1.78: `Editar todas` copia somente propriedades visuais selecionadas (Estilo/Animação/Posição) e nunca texto, IDs ou horários; confirmar e permitir desfazer condicionado ao snapshot. Edição individual preservada.
- v0.1.78: progresso Vosk baseado no áudio decodificado por `MediaCodec`, e não em timer; carga do modelo indeterminada, cronômetro apenas elapsed. Preservar cancelamento/recursos offline e `schemaVersion=4`.

- v0.1.79: defaults de legendas novas em `y=.92f` e gesto vertical até 0.95, sem reposicionar legendas já persistidas (`y` explícito). Prévia e exportação devem usar escala de texto adaptativa comum, sem truncar frases, sob validação de pixels no aparelho.
- v0.1.79: PiP opcional `opacity` 0–1 como campo adicional ao esquema 4 (default 1f em projetos antigos); aplicar a propriedade no Compose/TextureView e `PipCompositor.getOverlaySettings`, sem alterá-la no áudio nem mascarar os controles de edição.
- v0.1.79: ao fotografar quadro, pausar player antes de obter posição efetiva e manter pausado; salvar PNG original sem sobreposições, sem repetir captura, e continuar em dispatcher IO. Não confundir feedback de salvamento com processamento percentual real.

- v0.1.80: `SubtitleAutoFit.factor` deve garantir que a maior palavra caiba na largura disponível, mesmo quando precise reduzir abaixo de 48%. O piso de legibilidade de 48% restringe somente a redução decorrente do comprimento total da frase; `SubtitleExportSizing.relativeFontSize` não pode impor piso fixo que reintroduza corte no Media3. Manter testes `SubtitleAutoFitTest` e `SubtitleExportSizingTest` passando no CI antes de gerar APKs. Não alterar legendas persistidas ou `schemaVersion=4`.

- v0.1.81: dimensionar o lado menor do clipe de vídeo (Portrait/Landscape) por `Presentation.createForShortSide` ANTES do `TimedSubtitleOverlay` e da marca d'água; não adicionar `Presentation` global que reamostre os textos depois de renderizados. Preserve PiP e `schemaVersion=4`. Conferir aspecto e sincronismo em vídeos com resoluções/orientações diferentes no Android real antes de declarar fidelidade.
- v0.1.81: o relatório de exportação expõe somente métricas reais de `ExportResult` após sucesso do salvamento MediaStore; nunca confundir bitrate solicitado com bitrate médio efetivo, FPS médio estimado com FPS nominal, nem presumir ausência de recompressão social. `VideoSavedDialog` compartilhado mantém detalhes opcionais para Animar Imagens.

- v0.1.82: seleção de bitrate da exportação (Compacta/Alta/Máxima) é apenas parâmetro solicitado, nunca garantia de taxa obtida. O perfil padrão HIGH preserva 8 Mb/s (720p) e 18 Mb/s (1080p). Não reutilizar perfis para Animar Imagens sem pedido explícito.
- v0.1.82: `minimumFreeBytes` e a estimativa na UI devem usar o mesmo perfil entregue ao `ExportEngine` e encoder. `setiFrameIntervalSeconds(2f)` é solicitação sujeita ao codec, não evidência do GOP real no arquivo; não desativar fallback para forçar bitrate.
- v0.1.82: no diagnóstico `ExportQualityReport`, separar sempre taxa pedida da média medida. Bitrate abaixo do alvo pode ser efeito de conteúdo pouco complexo; não afirmar perda de qualidade sem inspeção do MP4. Manter 5 APKs Release e `schemaVersion=4`; validar em aparelho.
- v0.1.82: consulta da menor dimensão das fontes no seletor de exportação deve rodar em IO, somente como aviso não-bloqueante. Falha de metadados não impede renderização; orientação gira os eixos, mas não altera o lado menor. Estimativa do tamanho final usa duração do projeto e perfil selecionado e nunca é tamanho garantido.

- v0.1.83: revisar exportação anterior antes de declarar qualidade: resolução e bitrate são solicitações ao encoder, e métricas de `ExportResult` não substituem inspeção do MP4 final. Reservas/estimativas não devem truncar duração em 24h e devem usar aritmética saturada para entradas extremas. Mensagens de erro devem refletir o perfil realmente selecionado. Manter `schemaVersion=4`, cinco APKs, assinatura e demais recursos intactos.

- v0.1.84: `ExportEngine.start()` deve permanecer `internal` porque sua assinatura recebe `ExportBitratePreset` e retorna `ExportQualityReport`, ambos `internal`. O CI 70 reprovou a compilação Debug/Release ao expor esses tipos numa função `public`; a verificação estrutural deve proteger essa visibilidade. Preservar a API de chamada do ViewModel, demais parâmetros de exportação, schemaVersion 4, Secrets, assinatura e cinco APKs.

- v0.1.85: teste `WhatsNewReleaseNotesTest.reorderingOrChangingACaptionChangesTheFingerprint` deve usar pelo menos dois itens para verificar reordenação; `reversed()` de uma única novidade retorna conteúdo idêntico. Nunca acrescentar itens artificiais à UI só para passar no teste; validar separadamente alteração de título/descrição e preservar fingerprint SHA-256, política da tela Novidades e releases reais.

- v0.1.86: `SubtitleOutlineSpan` e `SubtitleShadowSpan` usam `TextOverlay.TEXT_SIZE_PIXELS`, que exige `@androidx.annotation.OptIn(UnstableApi::class)` nos dois pontos; manter import `androidx.annotation.OptIn` e não usar `kotlin.OptIn`, nem desativar o Lint ou criar baseline para ocultar erro. Workflow 72 passou pela compilação e assembleRelease, mas falhou em `:app:lintDebug` com 2 `UnsafeOptInUsageError`. Antes de anunciar cinco APKs, aguardar CI completo.

- v0.1.87: Animar Imagens acrescenta `ImageAnimationOptions.motion` opcional (`auto` legado / `zoom_in` / `zoom_out` / `horizontal` / `vertical`); manter o mesmo `ImageAnimationPlan.frame` na prévia e em `AnimatedImageMatrix`. Não modificar projetos de vídeo/schemaVersion 4. O rascunho JSON antigo sem `motion` assume `auto`; valores desconhecidos devem ser rejeitados sem apagar backup.
- v0.1.87: prévia mostra fundo da foto escurecido e foto central inteira, animados como uma única composição. Controles em bottom sheet, toque na foto seleciona instante, botão Rascunho não exporta, Gerar MP4 exporta sem obrigar à prévia. Ainda é preciso validar transições, EXIF, bitmaps, performance e MP4 no Android físico; não afirmar equivalência pixel-a-pixel.

- v0.1.88: manter gravação síncrona e validada em `ProjectStore` como salvamento primário. `ProjectBackupLifecycle` só agenda snapshots durante edição ativa e tenta gravar ao sair/ON_STOP; não prometer execução exata no background ou após morte do processo.
- v0.1.88: snapshots históricos `ProjectBackupStore` são JSON de configurações, não mídias; usar AtomicFile, SHA-256, limite de 2 MiB, nomes gerados de hashes dos IDs, retenção 3/5/10 e apenas alterações de conteúdo (ignorar campo updated). Corrompidos não podem sobrescrever cadastro anterior.
- v0.1.88: backup externo opcional requer pasta concedida por ACTION_OPEN_DOCUMENT_TREE, URI persistida e cópias distintas. Só limpar arquivos com prefixo exclusivo do aplicativo; não acessar Downloads ou mídias sem permissão explícita. Importação/exportação SAF exige confirmação antes de substituir versão.
- v0.1.88: MP4 final usa `GalleryVideoSaver`, destino `Movies/UaiCortes` em Android 10+; não confundir backup JSON com arquivo MP4. Não elevar `schemaVersion=4`; validar funcionalidade Android, backup externo e restore em aparelho antes de declarar pronta a versão.

- v0.1.89: preserve `SubtitleCue.wordTimings` only for synchronized Vosk captions; if text or timings are manually edited, clear outdated timing rather than pretend precise synchronization. `wordColors` indices refer to `Regex("\\S+")` and survive only if text unchanged. The highlight color and italic are optional JSON fields with legacy defaults.
- v0.1.89: `SubtitleWordEffects.colors` must be used identically by preview and export. `schemaVersion=4` unchanged. Animation screen Back returns Home regardless of entry route, but must honor unsaved drafts and active export. No claim of Android UI/pixel validation without a device build.

- v0.1.90: presets com `shadowOffsetDp=null` não sobrescrevem sombra previamente configurada; aqueles com sombra explícita aplicam seu valor. Manter `SubtitlePresentationTest` e novos testes de exportação/ASR.
- v0.1.90: tela Legendas exibe ações em grade compacta 2×3 (8 dp); exportar SRT/TXT via SAF não altera projeto nem inclui efeitos visuais; apagar todas exige confirmação e disponibiliza desfazer apenas quando lista segue vazia.
- v0.1.90: Gemini ASR deve usar apenas a chave do Keystore já existente, após consentimento explícito. Enviar WAV temporário até 10 min respeitando cortes da timeline e excluir arquivo local e remoto ao terminar. Nunca logar chave, conteúdo de áudio, transcrição bruta ou URL do upload. Vosk offline continua sem upload; `schemaVersion=4`.

- v0.1.91: legendas usam faixas LazyRow finitas (sem circular), com modelos animados e animação aplicada pelo preset; seletores de cor, fonte e efeito seguem o mesmo padrão horizontal. Preservar tempo, conteúdo e IDs na aplicação em lote.
- v0.1.91: a extração WAV de Gemini pode ser reutilizada APENAS com mesmo projeto, ordem, URI e recortes dos clipes (e revisão da mídia quando disponível). Cache privado `cacheDir/subtitle-audio`, no máximo 2 WAVs e TTL 3h, sem armazenamento de segredo; não apagar WAV reaproveitado ao finalizar a chamada. Envio à API continua somente com confirmação.
- v0.1.91: Vosk e Gemini geram novos cues em 20 sp com ajuste 12–30 sp na revisão; nenhum projeto legado ou legenda já salva é redimensionado automaticamente. Validar escala prévia/exportação no aparelho. `schemaVersion=4` preservado.
- v0.1.92: após falha CI77 do Android Lint (Range), nunca passar `getColumnIndex()` diretamente a `getLong()` sem validar índice >= 0 e, quando apropriado, testar valor nulo. Manter Lint habilitado e regression checker de `SubtitleAudioCache.kt`.

- v0.1.93: o preview animado dos diálogos deve permanecer fora da coluna verticalScroll em edição individual e lote; não duplicar preview dentro das opções. Barras B/I/tamanho são básicas, sem alterar dados persistidos. Grupos de palavras/posição/avançado continuam acessíveis e edição em lote mantém confirmação e sem mudanças em texto/tempo. Checar Lint/Gradle no CI.

- v0.1.94: na tela Legendas, a prévia do Media3 não pausa no fim do cue selecionado; ela continua pelos recortes até `Player.STATE_ENDED` ou pausa explícita. Durante Play, mostrar todos os cues ativos por intervalo semiaberto [startMs,endMs), com lacunas vazias e sobreposições preservadas; ao pausar, restaurar a edição do cue selecionado. Ao terminar, novo Play reinicia do começo do projeto. Preservar `schemaVersion=4` e a independência entre prévia e exportação.
- v0.1.94: Adicionar áudio mostra Escolher áudio + Gerar por IA na mesma linha, reutilizando o componente Gemini existente; remover o atalho Extrair redundante sem remover o painel Extrair áudio de vídeo. Não alterar API/vozes/prévias nesta versão. VersionCode 95; validar CI e aparelho antes de considerar APK pronto.

- v0.1.95: mostrar rótulo em português apenas para termos conhecidos, preservar nome original/ID sem traduzir identificadores do Gemini. Metadados são opcionais, nunca inferir gênero/timbre, e a busca inclui nome, ID, descrições e características quando presentes.
- v0.1.95: `ListVoices` não retorna `sample_audio` para catálogo prebuilt. O botão Ouvir amostra verifica cache privado primeiro, sem rede/síntese em hit; miss exige confirmação antes de sintetizar. Identidade de cache inclui chave, modelo, voz, estilo e frase fixa; nunca guardar chave em arquivo ou usar a fala do usuário como amostra. Limites 24 WAV/12 MiB/30 dias, arquivo válido e escrita atômica. Cache removível pelo sistema e não é biblioteca do projeto. Não implementar scraping de amostras do site.
- v0.1.95: versionCode 96, schemaVersion 4, 5 APKs Release sem publicação duplicada; compilar no CI e testar fisicamente antes de considerar finalizado.

- v0.1.96: 16 estilos de narração mantêm os cinco da v0.1.95 sem mudanças de instrução por padrão; ritmo, expressividade e entonação são orientações textuais em `speech_metadata.style`, nunca valores exatos de velocidade/frequência/volume. Não contaminar texto verbatim com instruções.
- v0.1.96: usar a mesma composição de estilo tanto em Ouvir amostra quanto em Gerar áudio, incluindo todas as dimensões na chave de cache derivada. Mudanças de ajuste exigem nova confirmação de síntese se não houver amostra idêntica. Preservar chave, consentimento, vozes, schemaVersion 4, cinco APKs, versionCode 97.

- v0.1.97: reutilização de legendas existentes (manuais/offline/Gemini) é somente leitura até confirmação; nenhum upload/ASR ou solicitação TTS no ato da importação. Mostrar trechos, seleção, horário e limite 4000 caracteres, sem truncar silenciosamente. Texto da narração é cópia editável separada das legendas originais.
- v0.1.97: abertura do gerador não consulta vozes nem escolhe automaticamente. Consulta explícita via botão; seleção da voz confirmada separadamente; gerar amostra/áudio exige consentimento de cota; inserir WAV na timeline exige confirmação adicional. Não adicionar faixa automaticamente. Preservar `schemaVersion=4`, versionCode 98, 5 APKs. Validar via Gradle/Android antes de declarar build pronto.

- v0.1.98: após falha do workflow 83 por empacotamento incompleto, gerar ZIPs de fonte exclusivamente com `python3 tools/package_source.py --output <arquivo.zip>`. O script exige `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, wrapper, workflow e ferramentas; extrai numa pasta temporária e roda `tools/validate_source.py` nela. Jamais entregar pacote se qualquer script Gradle faltar. Preservar 5 APKs Release no GitHub, versionCode sequencial 99 e documentação sincronizada.

- v0.1.99: `HomePrimaryActions` é componente único usado na Home e na prévia da tela Sobre; a prévia é estritamente somente leitura (`preview = true`), sem executar fluxos de projetos. Home e Sobre exibem a versão da mesma `AppReleaseNotes`.
- v0.1.99: versionamento também deve atualizar `github-manager.json`, `app_identity.json` e `VERSION`. `tools/check_version_sync.py` verifica `Gradle`, `workflow`, `package_apks`, `AppReleaseNotes`, metadados para GitHub Manager, e documentação; `tools/test_version_sync.py` comprova que divergências são rejeitadas. Atualizar sempre antes de empacotar o ZIP.
- v0.1.99: teste de UI `HomeAboutPreviewAndroidTest` requer Android real/emulador e não substitui verificação manual de navegação, criação de projetos e desempenho.

- v0.1.100: atualizações via GitHub Releases usam `GITHUB_REPOSITORY` como valor de build no CI; nunca inventar dono/repositório. Se local, permitir configuração manual. Consultar uma vez ao abrir, inclusive pré-releases, sem chamadas duplicadas por recomposição; nenhuma instalação/download automático.
- v0.1.100: exigir confirmação de download e instalação; validação SHA-256 quando GitHub informar digest, conferência de tamanho, packageName e versionCode; sem digest, somente abrir página oficial. Manter FileProvider privado e exigir assinatura persistente para atualização in-place. Verificação offline falha sem interromper editor. Preservar schemaVersion 4, versionCode 101 e metadados GitHub Manager.

- v0.1.101: não fixar versionName/versionCode em testes Android (usar BuildConfig); conferir mudanças antigas da Home no histórico de AppReleaseNotes, não em current.features. O workflow 85 teve 343 testes, 2 falhas em HomeAboutVersionContractTest.
- v0.1.101: ao mudar de versão, sempre sincronizar github-manager.json, app_identity.json, VERSION, Gradle, Home/Sobre, Novidades, CI, docs e nome do ZIP. A checagem de versão deve usar metadados reais, não string antiga fixada no validador.

- v0.1.102: tela Narração com IA permite diagnóstico manual da consulta Voices API com resposta sanitizada, sem persistir chave, transcrição, URL de credenciais ou resposta HTTP bruta. Falha do filtro pt-BR com HTTP 400/422 pode recorrer à listagem geral; não repetir em 401/403/429. Testes JVM com transporte simulado; testar em Android e com API real antes de afirmar carregamento corrigido. VersionCode 103, versão 0.1.102; preservar seleção/geração com confirmação e schemaVersion 4.

- v0.1.103: confirmar voz no seletor apenas atualiza o rascunho em memória, fecha os diálogos e volta ao editor; a prévia retoma a reprodução no cursor salvo. Não sintetizar, baixar nem inserir faixa nessa ação. `GeminiNarrationDraft` acompanha o projeto durante a sessão para preservar voz e texto quando a tela Áudio for recomposta. Cancelar não muda a voz. O seletor usa linha inteiramente clicável com rádio alinhado para não cortar o início do nome. `schemaVersion=4` e 5 APKs Release preservados; versionCode 104, metadados GitHub Manager sincronizados; validar Android real.

- v0.1.104: manter verificação de atualizações segura e diagnósticos categorizados (DNS, SSL, HTTP), sem incluir tokens, URL de conexão ou corpo de respostas nos erros visíveis; oferecer abertura manual das Releases para verificar repo público. Consulta continua automática com janela anti-flood; nunca baixar ou instalar sem confirmação. Sincronizar todos os metadados e versionCode.

- v0.1.105: prévia de Animar Imagens deve ler a foto real em proporção original sem usar `loadThumbnail` quadrado no painel principal, respeitar EXIF e limitar memória (lados longos até 1920 px), oferecer qualidade Rápida/Equilibrada/Alta e zoom de inspeção. Ajustes da prévia não alteram `ImageAnimationOptions` nem o MP4, e a composição com fundo escurecido permanece fiel ao exportador. Não adicionar campo ao schema do rascunho por causa da qualidade temporária da prévia. VersionCode 106 e metadados sincronizados.

- v0.1.106: escolher vozes apresenta uma única janela (ocultar a narração atrás do seletor), lista compacta com busca sem acentos, filtros por gênero somente quando `gender` é fornecido pela API e detalhes expansíveis sem tradução inventada da descrição oficial. Cancelar não altera a voz; confirmar retorna ao vídeo preservando o rascunho, sem síntese nem inclusão de áudio. Manter testes `GeminiVoiceCompactPickerTest` e `tools/test_voice_picker_contract.py`; validar visual de nomes extensos em Android. VersionCode 107 e GitHub Manager sincronizados.

- v0.1.107: filtro de gênero/pesquisa não pode confirmar uma voz previamente marcada, mas não visível na lista. `RadioButton` e linha devem permitir selecionar; escolha só pode ser confirmada se pertence ao resultado filtrado. O teste de amostra no seletor usa ID da voz marcada, sem aplicar a escolha antes de Confirmar, e pede autorização de cota se não estiver em cache. Preservar retorno ao vídeo sem TTS automático, versionCode 108 e metadados GitHub Manager. Testar voz feminina real e áudio Android antes de declarar timbre resolvido.

- v0.1.108: Extrair áudio de vídeo fica recolhido ao abrir a tela Áudio, salvo quando importação externa chega; manter ferramentas e formatos ao expandir, desabilitar recolhimento durante extração.
- v0.1.108: após confirmar **Adicionar à timeline** de narração Gemini, salvar o projeto antes de voltar ao editor. Só navegar se persistência retornar sucesso; preservar o editor e exibir aviso em caso de falha. Confirmar voz sozinho não chama a API nem insere faixa. Preservar schemaVersion 4 e versionCode 109.

- v0.1.109: confirmar uma voz no seletor volta à janela de narração, sem sair do editor nem chamar a API. Gerar e usar requer consentimento explícito para síntese e inserção; ao salvar com sucesso, fecha a janela Gemini e permanece na tela Áudio para conferir a faixa. Não duplicar URI na timeline; em falha ao salvar, manter WAV durável para tentar inserção sem nova síntese. Sem preflight GET /models que interrompa síntese válida.
- v0.1.109: botão Ouvir mixagem no topo da tela Áudio sincroniza original, música e faixas extras para prévia (não exporta nem modifica volumes). Botão Volume da prévia ao lado do Play afeta temporariamente todos os players no editor, incluindo PiP, mas nunca altera Clip.volume, AudioTrack.volume, Overlay.volume ou ExportEngine. `schemaVersion=4`, 5 APKs, versionCode 110 e todos os metadados sincronizados. Exigir build Android e teste no aparelho para validar áudio real.

- v0.1.110: tela Áudio mantém ação fixa "Salvar e voltar ao vídeo" após os painéis roláveis. Ela só navega após `write(changed)` bem-sucedido; exportação em andamento desabilita a ação e corte rascunho exige Salvar corte ou Cancelar antes. Persistência e falhas devem ser verificadas em aparelho.
- v0.1.110: títulos numéricos automáticos dos vídeos podem ter rótulos de exibição legíveis, sem alterar `Clip.title`; cartões e avisos de tela podem ser compactados sem modificar níveis da mixagem, schemaVersion 4 ou ExportEngine. versionCode 111; sincronizar GitHub Manager e CI.

- v0.1.111: manter as duas modalidades de TTS (contínua e por trechos). Trechos temporais preservam `SubtitleCue.startMs` e não usam cursor para posicionamento; cada WAV mantém a duração real. No modo temporal, revisão bloqueia áudio que exceda o projeto, solicita concordância para sobreposições e somente insere após salvar uma única cópia do projeto, sem truncagem automática. Confirmar cotas antes de cada lote de até 12 falas; em falha, preservar WAVs já válidos e reutilizar trechos inalterados. Nunca sobrescrever legendas nem mudar `schemaVersion=4`. Validar Gemini e preview/exportação em APK real antes de afirmar fidelidade.

- v0.1.112: recurso de referência sonora só orienta manualmente o tempo da narração Gemini por trecho. Usa vídeo, faixas já adicionadas ou arquivo SAF local. Nunca envie áudio de referência ao TTS, nunca prometa alinhamento automático ou cópia da voz sem suporte/consentimento; mantenha aprovação explícita e não altere os horários das legendas originais. Revisão deve usar inícios ajustados, detectar fim do projeto e sobreposição. Salvamento de WAVs continua independente da posição e evita repetir cota. `schemaVersion=4` mantido.

- v0.1.113: análise do áudio de referência é heurística de energia RMS (atividade sonora, NÃO detecção garantida de voz). Sem rede, sem upload e sob demanda apenas do trecho escolhido (até 15 s), com timeout e cancelamento. Música e efeitos podem causar falsos positivos; sugerir início somente próximo à legenda, nunca alterar o projeto sem “Usar sugestão” e “Aplicar horários”. Preservar demais recursos, WAVs, faixas, schemaVersion 4 e versionamento GitHub Manager.

- v0.1.114: Comparar A/B usa somente player local para escuta do áudio original do vídeo e do WAV Gemini; não modifica faixas, volume nem exportação. Metas de duração são instruções aproximadas, nunca edição real de PCM; conservar duração medida do WAV para revisão. Refazer uma fala exige confirmação e uma única nova chamada por toque, mantendo o WAV antigo em caso de erro e substituindo somente a referência do trecho após salvar o novo arquivo. Preservar schemaVersion 4 e validar em Android real.

- v0.1.115: gerar checkpoint durável por projeto da narração sincronizada (sem chaves/tokens), gravando após cada WAV válido antes da próxima solicitação. Restaurar somente quando o projeto/legendas e o SHA-256 do WAV são compatíveis; não refazer chamadas automaticamente. Confirmar cota para retomar, trabalhar em lotes de até 12, impedir reaproveitamento de WAVs truncados e preservar schemaVersion 4 e os controles de mixagem.

- v0.1.116: janela Gemini mantém geração/retomada visível em rodapé fixo com diagnóstico explícito de bloqueio. Editar uma única fala por vez, paginar até 12 legendas e navegar uma fala por vez na referência sonora. Não deixar a confirmação de chamadas paga oculta nem alterar a mixagem/legendas ao navegar. Preservar checkpoint dos WAVs, validação de tempo e versão sequencial.

- v0.1.117: tela VideoSavedDialog é compartilhada por Exportar e Animar Imagens. Exibir miniatura local e resumo técnico com detalhes recolhidos; não executar geração automaticamente. O botão Gemini exige consentimento e só transmite texto de contexto conhecido (título/legendas/assunto informado), nunca MP4, áudio, quadros, URIs nem chave. Validar 5 hashtags distintas, exatamente 100 caracteres no máximo incluindo quebra de linha, sem emojis/emoticons antes de Copiar/Compartilhar legenda. Preservar modo manual sem API, schemaVersion 4 e versionamento seq. Build e teste real continuam pendentes.

- v0.1.118: `app/src/androidTest/assets/export_smoke_16x9.mp4` é fixture sintético H.264/AAC usado somente por testes; não incluir em `src/main` nem remover faixas de áudio do teste. `ExportPipelineSmokeAndroidTest` deve verificar exportação Media3 com recorte/legenda/áudio e limpar URI do MediaStore após teste. O CI compila os fontes de AndroidTest e executa todos os `tools/test_*.py`; compilação NÃO executa o smoke test em aparelho. Preservar cinco APKs Release, assinatura, schemaVersion 4 e versionamento. Não declarar exportação validada antes de rodar o instrumentado com hardware compatível.

- v0.1.119: o editor desenha o último PiP por cima; Media3 desenha a primeira entrada de vídeo por cima, portanto `ExportLayerOrder.media3TopFirst` inverte SOMENTE a ordem das entradas de composição, jamais o projeto salvo. Quando há PiP, `Composition.Builder.setEffects` aplica legendas/marca d'água depois do compositor, sobre as janelas. Sem PiP, conservar texto por clipe. `SubtitlePreviewPolicy.firstAt` escolhe a primeira legenda não vazia como o exportador. `TransitionEffects.motionAt` é a única regra para zoom/pan no editor e no Media3. Validar em APK real com dois PiPs sobrepostos, legendas sobre PiP, cortes sequenciais, vídeos 9:16 e transições; smoke Android compilado não é teste executado. Não afirmar fidelidade total de cor/som, pois prévia tonal continua aproximada. Schema 4, 5 APKs e versionCode sequencial preservados.


- v0.1.120: cinco módulos Media3 (ExoPlayer/UI/Transformer/Effect/Common) na mesma versão estável 1.11.1. Nunca habilitar MP4 edit-list trim sem considerar risco de expor quadros ocultos; trim optimization só pode ser discutido para single-asset MP4 sem efeitos e com contrato de qualidade preservado. `ExportQualityReport` separa tempo real de codificação e cópia MediaStore (relógio monotônico) e não promete aceleração sem benchmark. `GalleryVideoSaver` usa blocos de 256 KiB e deve conservar contagem integral, `IS_PENDING` e rollback. CI precisa validar Media3, sem alegar build ou teste Android até execução real. VersionCode 121, schemaVersion 4, cinco APKs mantidos.

- v0.1.121: o CI100 falhou em `:app:compileDebugAndroidTestKotlin` por `import androidx.compose.ui.test.assertExists` inexistente. `assertExists()` é membro da interação semântica; não importar o nome top-level. Preservar `HomeAboutPreviewAndroidTest`, rodar `tools/test_ci100_compose_import_contract.py` no loop do CI e confirmar o Gradle completo antes de afirmar que APKs Release foram gerados. VersionCode 122, schemaVersion 4, Media3 1.11.1, cinco APKs e assinatura inalterados.
